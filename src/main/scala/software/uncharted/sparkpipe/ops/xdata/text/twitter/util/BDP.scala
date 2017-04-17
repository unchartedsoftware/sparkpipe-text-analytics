/**
  * Copyright (c) 2014-2015 Uncharted Software Inc. All rights reserved.
  *
  * Property of Uncharted(tm), formerly Oculus Info Inc.
  * http://uncharted.software/
  *
  * This software is the confidential and proprietary information of
  * Uncharted Software Inc. ("Confidential Information"). You shall not
  * disclose such Confidential Information and shall use it only in
  * accordance with the terms of the license agreement you entered into
  * with Uncharted Software Inc.
  */

package software.uncharted.sparkpipe.ops.xdata.text.twitter.util

import grizzled.slf4j.Logging
import org.apache.spark.broadcast.Broadcast


/**
  * BACKGROUND
  *   Topic modelling is a machine learning method used to infer the latent structure in a collection of documents, uncovering
  *   hidden topic patterns. It is important to note that the topics discovered by topic modelling are latent. That is, topic
  *   modelling allows us to uncover topic-term probabilities, (i.e. the top words associated with a topic) but *not* the
  *   topic itself. Topic modelling is uses to (automatically) organize, summarize and/or understand a large corpus.
  *
  *   Latent Dirichlet Allocation (LDA) is a popular algorithm for topic modelling, which performs well on a corpus of lengthy
  *   documents. However, the data sparsity in short texts causes very noisy LDA models. To get around this issue we have
  *   implemented an extension of LDA for short texts called Biterm Topic Modelling (BTM).
  *
  * BTM
  *   The BTM model is a generative model which learns topics over short texts by directly modelling the generation of biterms
  *   (co-occurring terms) across the corpus. Two key differences from the traditional (e.g. LDA) approaches are:
  *     (1) word co-occurrence patterns are explicitly
  *     (2) global co-occurrence patterns are modeled
  *
  * BDP
  *   Biterm Topic Model, Dirichlet Process (BDP) is an extension to the BTM algorithm. BTM is a parametric algorithm (i.e.
  *   the number of expected clusters must be specified a priori). BDP is a nonparametric extension which allows an unbounded
  *   number of clusters to be discovered by the algorithm.
  *
  *  The key changes (from BTM) are:
  *     Sample Recorders:
  *         the nonparametric Bayesian approach allows for unbounded values of K. This means
  *         that the sample recorders can expand (and contract!) to hold different numbers
  *         of 'topics'. As a result sample recorders to use MutableArrays rather than Arrays
  *         (as in Xiaohui Yan's implementation)
  *     Markov Chain Monte Carlo (MCMC) sampling:
  *         n.b. we use an MCMC approach (Gibbs sampling) to estimate the conditional posterior for an unbounded K
  *           draw topic index z using Chinese Restaurant Process + stick breaking
  *           if z = k_new, increment the number of topics (tables), add to sample recorders
  *           if nz(z) == 0 (i.e. if there are no samples assigned to topic z) remove the topic (table), remove from sample recorders
  *           returns K, theta, phi (standard BTM returns theta, phi)
  *
  * BDP ALGORITHM
  *   BDP infers two parameters from a corpus:
  *     -   ϕ (phi):    topic-word distribution (i.e. the probability of a word w, given a topic z)
  *     -   θ (theta):  global topic distribution (i.e. the probability of a topic z)
  *
  *   Phi
  *     ϕ_w|k = (n_w|z + η) / (∑ n_w|z + Nη)
  *
  *   Theta
  *     θ_z = (n_z + α) / (|B| + Kα )
  *
  *     Where:
  *       N:        number of words in the vocabulary
  *       w|k:      probability of word w given topic z
  *       n_w|z:    number of word w given topic z
  *       n_z:      number of topic z
  *       α:        Dirichlet hyperparameter
  *       |B|:      number of biterms
  *       K:        number of topics
  *
  *   INPUT: biterm set B, hyperparameters α, η
  *   OUTPUT: number of topics K, multinomial parameters theta, phi
  *   REPEAT
  *     FOR x ∈ B DO
  *       Draw the topic index k from the conditional posterior for k (Gibbs sampling)
  *       IF k = k_new THEN
  *           K = K + 1
  *       END IF
  *       Update n_z, n_w2|z, n_w2|z
  *     END FOR
  *     Compute ϕ and θ
  *   UNTIL iter = N_iter
  *   RETURN K, ϕ, θ
  *
  * References:
  *   https://github.com/xiaohuiyan/BTM
  *   http://www2013.org/proceedings/p1445.pdf
  *   http://www.bigdatalab.ac.cn/~lanyanyan/papers/2014/TKDE-yan.pdf
  *
  **/
class BDP(kk: Int) extends Serializable with Logging {
  var k = kk
  var tfidf_dict: Map[Int,Double] = Map[Int,Double]()

  // ============================= Markov Chain Monte Carlo (MCMC)  ============================================
  def estimateMCMC(biterms:Array[Biterm], iterN: Int, model: SampleRecorder, m: Int, alpha: Double, eta: Double): (Int, Double) = {
    val start = System.nanoTime
    Iterator.range(0, iterN).foreach { iteration =>
      info(s"iteration: ${iteration + 1}\tk = $k")
      val bstart = System.nanoTime
      biterms.foreach {
        case b : Any => updateBiterm(b, model, m, alpha, eta)
      }
      // removeEmptyClusters & defrag
      k = model.defrag()
      val bend = System.nanoTime
      val runningtime = (bend - bstart) / 1E9
      info("\t time: %.3f sec.".format(runningtime))
    }
    val duration = (System.nanoTime - start) / 1E9 / 60.0
    info(s"Elapsed time: %.4f min".format(duration))
    (k, duration)
  }

  def updateBiterm(b: Biterm, model: SampleRecorder, m: Int, alpha: Double, eta: Double) : Unit = {
    unsetTopic(model, b)
    val z = drawTopicIndex(model, b, m, alpha, eta)

    // increment value of K if z > k
    if (z == k) {
      info("\t+1\t")
      k = model.addCluster()
    }
    if (z >= k) info(s"\n\n WHY IS Z > K ? \nz is $z and k is $k\n")
    if (z > k) info("\n\n********\n z should not be greater than k !!!\n\n")
    setTopic(model, b, z)
  }

  def setTopic(model: SampleRecorder, b:Biterm, z:Int) : Unit = {
    b.z = z
    model.increment(z, b.biterm)
  }

  def unsetTopic(model: SampleRecorder, b:Biterm) : Unit = {
    model.decrement(b.biterm, b.z, k)
  }


  // =============================   MCMC Sampling   =============================
  def drawTopicIndex(model: SampleRecorder, b: Biterm, m: Int, alpha: Double, eta: Double) : Int = {
    val dist = conditionalPosterior(model, b.biterm, m, alpha, eta)
    sample(dist)
  }

  // calculate PDF: the conditional posterior for all k (i.e. the likelihood of generating biterm b)
  def conditionalPosterior(model: SampleRecorder, b: (Int, Int), m: Int, alpha: Double, eta: Double) : Array[(Double, Int)] = {
    val (w1, w2) = b
    // calculate f(•), the density of Mult(z) created by CRP
    def density(z: Int) = {
      val numerator = (model.getNwz(z, w1) + eta) * (model.getNwz(z, w1) + eta)
      val denominator = Math.pow( 2 * model.getNz(z) + m * eta, 2)
      val d = numerator.toDouble / denominator.toDouble
      model.getNz(z) * d
    }
    def densityNew() = {
      val numerator = ( 0 + eta) * ( 0 + eta)
      val denominator = Math.pow(  0 + (m * eta), 2)
      alpha * (numerator / denominator)
    }
    val pd_existing = Iterator.range(0, k).map{ z => density(z) }.toArray
    val pd_new = densityNew()
    val dist = pd_existing ++ Array(pd_new)
    val total = dist.sum
    val norm_dist = dist.map(_ * ( 100 / total / 100))
    norm_dist.zipWithIndex.sortWith(_._1 > _._1) // associate each prob with its topic index, sort descending to make sampling more efficient
  }

  // scalastyle:off return
  def sample[A](dist: Array[(Double, A)]): A = {
    val p = scala.util.Random.nextDouble
    val it = dist.iterator
    var accum = 0.0
    while (it.hasNext) {
      val (itemProb, item) = it.next
      accum += itemProb
      if (accum >= p) {
        return item  // return so that we don't have to search through the whole distribution
      }
    }
    sys.error(f"this should never happen")  // needed so it will compile
  }

  // ============================= Estimate parameters  =============================
  def calcTheta(model: SampleRecorder, n_biterms: Int, k: Int, alpha: Double) : Array[Double] = {
    Iterator.range(0, k).map { z =>
      (model.getNz(z) + alpha) / (n_biterms + k * alpha)
    }.toArray
  }

  def calcPhi(model: SampleRecorder, m: Int, k: Int, beta: Double) : Array[Double] = {
    Iterator.range(0, m).flatMap { w =>
      Iterator.range(0, k).map { z =>
        (model.getNwz(z, w) + beta) / (model.getNz(z) * 2 + m * beta)
      }
    }.toArray
  }

  def estimateThetaPhi(model: SampleRecorder, n_biterms: Int, m: Int, k: Int, alpha: Double, beta: Double) : (Array[Double], Array[Double]) = {
    (calcTheta(model, n_biterms, k, alpha ), calcPhi(model, m, k, beta ))
  }

  /**
    * Initialize this BDP instance's tfidf dictionary/map with the input map.
    * Filter out dates outside of the given and filter out words not in the given word dictionary (associated with the given date)
    * @param tfidf_bcst the tfidf data
    * @param date the date to filter tfidf by
    * @param word_dict the word map to filter tfidf by
    */
  def initTfidf(
    tfidf_bcst: Broadcast[Array[(String, String, Double)]],
    date: String,
    word_dict: Map[String, Int]
  ) : Unit = {
    val tfidf = tfidf_bcst.value
    val tfidf_date_filtered = TFIDF.filterDateRange(tfidf, Array(date))
    val tfidf_word_filtered = TFIDF.filterWordDict(tfidf_date_filtered, word_dict)
    tfidf_dict = TFIDF.tfidfToMap(tfidf_word_filtered, word_dict)
  }

  /**
   * The main method of Biterm Topic Modelling, fits a model to the input data.
   * @param biterms   An Array of co-occuring terms
   * @param words     An Array of terms representing the vocabulary of the model
   * @param iterN     Number of iterations of MCMC sampling to run
   * @param k         Number of topics to start with
   * @param alpha     Dirichlet hyperparameter of the clumpiness of the model
   * @param eta       Dirichlet hyperparameter of the distribution of the underlying base distribution
   * @param weighted  Boolean flag controlling whether to use TFIDF weights or not
   * @param topT      Number of top terms per topic to return
   * @return          A tuple containing topic_dist (an array containing phi and topT words for each topic),
   *                  theta & phi (model parameters inferred by MCMC sampling), a map containing (topic -> count) for each topic,
   *                  and the running time in seconds.
   */
  // scalastyle:off magic.number
  def fit(biterms: Array[Biterm],
    words: Array[String],
    iterN: Int,
    k: Int,
    alpha: Double,
    eta: Double,
    weighted: Boolean = false,
    topT: Int = 100
  ) : (Array[(Double, Seq[Int])], Array[Double], Array[Double], Map[Int,Int], Double) = {
    val m = words.length
    val SR = new SampleRecorder(m, k, weighted)
    if (weighted) {
      SR.setTfidf(tfidf_dict)
    }
    SR.initRecorders(biterms)
    val btmDp = new BDP(k)
    info("Running MCMC sampling...")
    val (newK, duration) = estimateMCMC(biterms, iterN, SR, m, alpha, eta)
    val n_biterms = biterms.length
    info("Calculating phi, theta...")
    val (theta, phi) = estimateThetaPhi(SR, n_biterms, m, newK, alpha, eta )
    info("Calculating topic distribution...")
    // take top words for a topic (default is top 100 words)
    val topic_dist = BTMUtil.reportTopics(theta, phi, words, m, newK, topT)
    val nzMap = SR.getNzMap().toMap[Int, Int]
    (topic_dist, theta, phi, nzMap, duration)
  }
}
