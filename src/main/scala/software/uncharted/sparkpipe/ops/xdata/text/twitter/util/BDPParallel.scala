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

import java.io.Serializable

import grizzled.slf4j.Logging
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DoubleType, MapType, StringType, StructType, StructField}

/**
  * This is an extension of BDP (which itself is an extension of BTM, which is a variant on LDA)
  * BDPParallel extends BDP with pseudo-parallelism. The BDP algorithm is run model-parallel over
  * different partitions of data. A DatePartitioner is uses to partition data by date.
  *
  * Within each partition, the BDP algorithm is run on the partitioned data and an iterator of BDP
  * results returned
  */
object BDPParallel extends Serializable with Logging {

  /**
    * Get the updated schema for the topic modelled data frame.
    * @param existingSchema Schema of the topic modelling input.
    * @param outputCol Name of the output column that contains the topic modelling results.
    * @return The updated schema for the modelled data frame.
    */
  def getTweetTopicSchema(existingSchema: StructType, outputCol: String) : StructType = {
    StructType(
      existingSchema.fields ++ Array(
        StructField(outputCol, MapType(StringType, DoubleType))
      )
    )
  }

  /**
    * Run the topic modelling algorithm across a single partition of data.
    * @param iterator The data in the partition.
    * @param stpbroad Set of stop words.
    * @param iterN The number of iterations of MCMC sampling to run.
    * @param k The number of topics to start with.
    * @param numTopics Number of topics to output.
    * @param alpha A dirichlet hyperparameter of the clumpiness of the model.
    * @param eta
    * @param textCol The column of the input DataFrame in which to find the text
    * @param dateCol The column of the input DataFrame in which to find the date
    * @param idCol The column of the input DataFrame in which to find the id
    * @param tfidf_bcst TFIDF data to use when scoring words.
    * @return The input rows with a new column containing the topics and their probabilities.
    */
  // scalastyle:off parameter.number
  def partitionBDP(
                    iterator: Iterator[Row],
                    stpbroad: Broadcast[scala.collection.immutable.Set[String]],
                    iterN: Int,
                    k:Int,
                    numTopics: Int,
                    alpha: Double,
                    eta: Double,
                    textCol: String,
                    dateCol: String,
                    idCol: String,
                    tfidf_bcst: Option[Broadcast[Array[(String, String, Double)]]] = None
                  ) : Iterator[Row] = {
    if (iterator.isEmpty) {
      info("Empty partition. Revisit your partitioning mechanism to correct this skew warning")
      Iterator.empty
    } else {
      //Extract text from data.
      val data = iterator.toSeq
      val datetexts = data.map(x => (x(x.fieldIndex(dateCol)), x(x.fieldIndex(textCol))))
      val date = datetexts.head._1.asInstanceOf[String]
      val texts : Array[String] = datetexts.map(x => x._2.asInstanceOf[String]).distinct.toArray // no retweets

      //Get the word counts.
      val stopwords = stpbroad.value
      val minCount = 0
      val (word_dict, words) = WordDict.createWordDictLocal(texts, stopwords, minCount)

      //Run the BDP.
      val bdp = new BDP(k)
      val biterms = texts.map(text => BTMUtil.extractBitermsFromTextRandomK(text, word_dict, stopwords, k)).flatMap(x => x)

      var weighted = false
      if (tfidf_bcst.isDefined) {
        bdp.initTfidf(tfidf_bcst.get, date, word_dict)
        weighted = true
      }

      val (topic_dist, theta, phi, nzMap, duration) = bdp.fit(biterms, words, iterN, k, alpha, eta, weighted)

      //Label the documents with the relevant topics.
      labelDocuments(data, word_dict, stopwords, theta, phi, k, numTopics, textCol, dateCol, idCol)
    }
  }

  private def labelDocuments(
                      data: Seq[Row],
                      wordDict: Map[String, Int],
                      stopWords: scala.collection.immutable.Set[String],
                      theta: Array[Double],
                      phi: Array[Double],
                      k: Int,
                      numTopics: Int,
                      textCol: String,
                      dateCol: String,
                      idCol: String
                    ) : Iterator[Row] = {
    //Create the word lookup as the inverse of the word dictionary.
    val wordLookup = wordDict.map(_.swap)

    data.map(doc => labelDocument(doc, wordDict, wordLookup, stopWords, theta, phi, k, numTopics, textCol, dateCol, idCol)).toIterator
  }

  private def labelDocument(
                      document: Row,
                      wordDict: Map[String, Int],
                      wordLookup: Map[Int, String],
                      stopWords: scala.collection.immutable.Set[String],
                      theta: Array[Double],
                      phi: Array[Double],
                      k: Int,
                      numTopics: Int,
                      textCol: String,
                      dateCol: String,
                      idCol: String
                    ) : Row = {
    //Extract the biterms from the row.
    val tokens = TextUtil.prep(document(document.fieldIndex(textCol)).asInstanceOf[String]).toArray
    val d = BTMUtil.getWordIds(tokens, wordDict, stopWords)
    val bs = BTMUtil.getBiterms(d).toArray

    //Calculate the probabilities.
    val limit = Math.min(k, theta.length)
    val hs = bs.map { b =>
      val (w1, w2) = b
      1.0 / Iterator.range(0, limit).map { z =>
        theta(z) * phi(w1 * limit + z) * phi(w2 * limit + z)
      }.sum * bs.count(_ == b) / bs.length
    }

    val p_z_d = Iterator.range(0, limit).toArray.map{ z =>
      bs.zip(hs).map { case (b, h) =>
        val (w1, w2) = b
        theta(z) * phi(w1 * limit + z) * phi(w2 * limit + z) * h
      }.sum
    }

    //Retain the top N topics.
    val topics = p_z_d.zipWithIndex
      .sortBy(p => -p._1)
      .map(p => (wordLookup(p._2), p._1))
      .take(numTopics)
      .toMap
    Row.fromSeq(document.toSeq ++ Array(topics))
  }

  /**
    * Utility function
    * Takes in an RDD of string array of size three
    *
    * @param rdd
    * @return RDD with the original array split into string Tuple2
    */
  def keyvalueRDD(rdd: RDD[Array[String]]) : RDD[(String, (String, String))] = {
    rdd.map(x => (x(0), (x(1), x(2))))
  }
}
