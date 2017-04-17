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
import scala.io.Source

/**
  * A library of functions to manipulate text files
  */
object WordDict extends Serializable with Logging {

  def createDictFromArray(words: Array[String]) : Map[String, Int] = {
    Array.tabulate(words.length)(i => words(i) -> i).toMap
  }

  def wordcount2WordDict(
    wordcount: Array[(String, Int)],
    stopwords: Set[String], min_count: Int = 1
  ) : (Map[String, Int], Array[String])= {
    val filtered = wordcount.filter{case (word, count) => word.length > 2 }              // exclude words with 1-2 characters
                            .filter{case (word, count) => count >  min_count }            // exclude words with counts under min_count
                            .filterNot{case (word, count) => "^[#0-9]+$".r.findFirstIn(word).isDefined }   // exclude words made up of only digits
                            .filterNot{case (word, count) =>
                              // exclude words made up of only punctuation
                              "^[\\s!\"#$%&\\\\\\'()*+,-./:;<=>?@\\[\\]^_`{|}~]+$".r.findFirstIn(word).isDefined  }
                            .filterNot{case (word, count) => stopwords contains word }       // exclude stopwords
    val sorted = filtered.sortWith{ (a, b) => a._2 > b._2 }
    val words = sorted.map{case (word, count) => word}
    val word_dict = createDictFromArray(words)
    (word_dict, words)
  }

  def computeWordCountLocal(
    arr: Array[String],
    minCount: Int = 5
  ) : Array[(String,Int)] = {
    // extract text only, clean text of punctuation & lowercase
    val cleaned = arr.map(x => TextUtil.cleanText(x) )
    // find out which words we REALLY need in the word_dict (to make it smaller)
    val tokens = cleaned.map(x => x.split("\\s+")).flatMap(x => x)
    tokens.groupBy(_.toString).map(x => (x._1, x._2.size)).filter(x => x._2 > minCount).toArray
  }

  /**
    * Read in the stopwords (words to be ignored)
    *
    * @param swfiles The list of stopword files
    * @return A set of all stopwords
    */
  def loadStopwords(swfiles: List[String]) : Set[String] = {
    swfiles.flatMap(path => Source.fromFile(path).getLines).toSet
  }

  /**
    * Create a dictionary/map of words and their counts
    * @param arr source of words to aggregate
    * @param stopwords Words to ignore
    * @param minCount The theshold number of occurances of a word to be permitted in the final dictionary
    * @return
    */
  def createWordDictLocal(
    arr: Array[String],
    stopwords: Set[String],
    minCount: Int
  ) : (Map[String, Int], Array[String]) = {
    val wc = computeWordCountLocal(arr, minCount)
    info(s"wc contains ${wc.length} words")
    info("computing word dictionary...")
    wordcount2WordDict(wc, stopwords, minCount)
  }
}
