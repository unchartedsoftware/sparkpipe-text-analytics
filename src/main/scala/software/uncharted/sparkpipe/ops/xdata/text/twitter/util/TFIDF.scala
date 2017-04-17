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

import org.apache.spark.sql.DataFrame

/**
 * NOTE: TFIDF scores are not used in the current implementation. For prototyping
 * TFIDF scores were pre-computed and then loaded using the methods below.
 *
 * PURPOSE:
 *    TFIDF weights may be used to increase or decrease the influence of terms withing
 *    the model. The motivation for this exploration was noticing that terms such as
 *    "isis" appear in many topics. However, since that term appears with high frequency
 *    throughout our keyword-filtered corpus, it is un-informative. Rather than the biterm
 *    sample recorders incrementing/decrementing counts by +/-1 TFIDF weights could be used.
 *    This would have the effect of (1) severely diminishing the influence of corpus-wide terms
 *    such as "isis", while at the same time boosting the influence of terms which are high frequency
 *    on a given date but not otherwise. An extension of this basic idea was to use TFIDF score from
 *    a sliding window of time (e.g. 1 week) to boost the influence of terms which are important
 *    within the days before and after a given date.
 *
 * WHY TFIDF IS NOT USED:
 *    It takes an extra computational step to compute tfidf scores. Although experimental
 *    results suggested that this technique is a useful way of boosting topical terms and
 *    decreasing overly-frequent terms it did not seem to be enough of a benefit to offset the
 *    extra computation required. Additionally, since our Twitter corpus is already filtered
 *    by keyword, it is easier to simply add the highest frequency keywords to a stopword list.
 *
 * In order to use TFIDF weighting of terms it will be necessary to extend this class to
 * compute TFIDF scores over the entire input corpus. For the prototype this was done
 * in two ways:
 *    (1)   consider the concatenated tweets from a given date as a document
 *    (2)   consider the concatenated tweets from a sliding window of dates as a document
 *        assigning the resultant score to the date at the mid-point of the sliding window.
 */
object TFIDF extends Serializable {

  /**
  * Load pre computed tfidf scores from a DataFrame
  * @param input The DataFrame from which to read
  * @return an Array in the form of (date, word, score)
  */
  def loadTFIDF(input: DataFrame): Array[(String, String, Double)] = {
    input.collect.map{row => (
      row(0).toString,
      row(1).asInstanceOf[String], // word
      row(2).asInstanceOf[Double] // score
    )}
  }

  /**
   * Filter an array of TFIDF scores to a given set of dates
   * @param tfidf   An Array with schema (date, term, score) for TFIDF scores of words
   * @param dates   An Array of dates with format YEAR-MONTH-DATE (e.g. 2016-02021)
   * @return        An Array with schema (date, term, score) for all days in dates
   */
  def filterDateRange(
    tfidf: Array[(String, String, Double)],
    dates: Array[String]
  ): Array[(String, String, Double)] = {
    tfidf.filter{case (d, w, s) => dates contains d}
  }

  /**
   * Filter an array of TFIDF scores for terms in word_dict
   * @param tfidf     An Array with schema (date, term, score) for TFIDF scores of words for given dates
   * @param word_dict A Map of word -> count
   * @return          The input array containing only terms in word_dict
   */
  def filterWordDict(
    tfidf: Array[(String, String, Double)],
    word_dict: Map[String, Int]
  ): Array[(String, String, Double)] = {
    tfidf.filter{case (d, w, s) => word_dict contains w }
  }

  /**
   * Convert a tfidf array to a map where the key is the word's count, and the value is the word's tfidf score. The word itself is left out
   * @param tfidf     An Array with schema (date, term, score) for TFIDF scores of words for given dates
   * @param word_dict A Map of word -> count
   * @return          A map of word counts and their scores
   */
  def tfidfToMap(
    tfidf: Array[(String, String, Double)],
    word_dict: Map[String, Int]
  ): Map[Int,Double] = {
    tfidf.map{case (d, w, s) => (word_dict.get(w).get, s)}.toMap
  }
}
