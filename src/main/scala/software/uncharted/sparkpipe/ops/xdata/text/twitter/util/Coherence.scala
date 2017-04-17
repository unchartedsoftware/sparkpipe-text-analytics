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
import org.apache.spark.rdd.RDD
import scala.io.Source
import org.apache.spark.sql.DataFrame
import scala.math.log

object Coherence extends Serializable with Logging {

  def getTokensRdd(rdd: RDD[String]) : RDD[Array[String]] = {
    rdd.map(text => TextUtil.cleanText(text)).map(_.split("\\s+"))
  }

  def getVocabFromTopicDist(topic_dist: Array[(Double, Seq[String])] , topT: Int) : Set[String] = {
    topic_dist.map{ case (theta, tpcs) => tpcs }.map(x => x.take(topT)).flatMap(x => x).toSet
  }

  def getTopicsFromTopicDistFile(topic_dist_file: String, termIdx: Int): Array[Array[String]] = {
    Source.fromFile(topic_dist_file).getLines.filter(x => !x.startsWith("#")).map(_.split("\t")).map(x => x(termIdx)).map(x => x.split(",\\s?")).toArray
  }

  def topTwordsFromTopics(topics: Array[Array[String]], topT: Int) : Set[String] = {
    topics.flatMap(x => x.take(topT)).toSet
  }

  // Nathan's improvement on the function below - doesn't involve Java boxing - unboxing
  def toDouble (number: Any): Double = number match {
    case n: Double => n
    case n: Int => n.toDouble
  }

  // Cast a number which is AnyType to Double
  def castDouble(number: Any): Double = number match {
    case n: Double => n.doubleValue()
    case n: Int    => n.toDouble
    case x: Any    => throw new IllegalArgumentException(s"$x is not a number.")
  }

  // document frequency of a word
  def unigramDocFreq(tokens: RDD[Array[String]], vocabulary: Set[String]) : Map[String, Int] = {
    tokens.map(x => x.filter(vocabulary contains _)).filter(x => x.size > 0).map(x => x.toSet.toSeq).flatMap(x => x).map(x => (x, 1)).reduceByKey(_ + _).collect.toMap
  }

  // document frequency of a bigram
  def bitermDocFreq(tokens: RDD[Array[String]], vocabulary: Set[String]) : Map[(String, String), Int] = {
    def getWordBiterms(d: Array[String]): Iterator[(String, String)] = {
      val bt = d.combinations(2).map { case Array(w1, w2) => if (w1 < w2) (w1, w2) else (w2, w1) }
      bt
    }
    val biterms = tokens.map(x => x.filter(vocabulary contains _)).filter(x => x.length > 1).map(x => getWordBiterms(x))
    info(s"\n biterms.size = ${biterms.count}\n")
    val bi_wc = biterms.flatMap(x => x).map(x => (x, 1)).reduceByKey(_ + _).filter(x => x._2 > 1)
    info(s"bi_wc ... bi_df ...")
    val bi_df = bi_wc.collect.toMap
    info(s"bi_df.size = ${bi_df.size}")
    bi_df
  }

  def topicCoherence(topic: Array[String], unigram_df: Map[String,Int], biterm_df: Map[(String, String),Int], epsilon: Double = 1 ) : Double = {
    val cowords = topic.toSeq.combinations(2).toArray
    cowords.map { case Seq(w1, w2) =>
      val numerator = biterm_df.getOrElse(if (w1 < w2) (w1, w2) else (w2, w1), 0) + epsilon
      val denominator = unigram_df.getOrElse(w1, numerator)
      val c = log(castDouble(numerator) / castDouble(denominator))
      c
    }.sum
  }

  /**
   * @param rdd          RDD containing JUST the cleaned text
   * @param topic_terms  the top words (just the words, not theta) returned by running topic modelling (output as topic_dist)
   * @param topT         the top T words which to consider in computing coherence scores
   * @return       (Array of coherence scores (one per topic), average coherence score for input data)
   */
  def computeCoherenceHelper(rdd: RDD[String], topic_terms: Array[Array[String]], topT: Int) : (Seq[Double], Double) = {
    val topT_topics = topic_terms.map(x => x.take(topT)).toSeq
    val vocabulary = topTwordsFromTopics(topic_terms, topT)
    // convert documents into token arrays
    val tokens = getTokensRdd(rdd).map(x => x.filter(vocabulary contains _)).cache
    // compute D(v) and D(V, v'), return a Map of each
    info("calculating unigram doc freq...")
    val unigram_df = unigramDocFreq(tokens, vocabulary)
    info("calculating biterm doc freq...")
    val biterm_df = bitermDocFreq(tokens, vocabulary)
    info("calculating topic coherence...")
    val cs = topT_topics.map{tpcs  => topicCoherence(tpcs, unigram_df, biterm_df)
    }
    val avg_cs = cs.sum / cs.size
    (cs, avg_cs)
  }

  def computeCoherence (
    cparts : Array[(String, Array[(Double, Seq[String])], Array[Double], Array[Double], Map[Int,Int], Int, Double)],
    data: DataFrame,
    numTopTopics : Int,
    textCol : String
  ) : Map[String, (Seq[Double], Double)] = {
    var result : scala.collection.mutable.Map[String, (Seq[Double], Double)] = scala.collection.mutable.Map()
    cparts.foreach { cp =>
      val (date, topic_dist, theta, phi, nzMap, m, duration) = cp
      val topic_terms = topic_dist.map(x => x._2.toArray)
      val textrdd = data.select(textCol).rdd.map(r => r(r.fieldIndex(textCol)).toString)
      val (_cs, _avg_cs) = Coherence.computeCoherenceHelper(textrdd, topic_terms, numTopTopics)
      result += ((date, (_cs, _avg_cs)))
    }
    result.toMap // make immutable
  }
}
