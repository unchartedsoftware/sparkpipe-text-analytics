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
package software.uncharted.sparkpipe.ops.text

import java.io.FileInputStream

import org.apache.spark.mllib.linalg.{SparseVector, Vector}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import software.uncharted.sparkpipe.ops.text.analytics.DictionaryConfig

import scala.collection.mutable.{Map => MutableMap}
import scala.reflect.ClassTag

/**
  * Various generic text operations, not specific to a single textual analytic
  */
package object transformations {

  val notWord = "('[^a-zA-Z]|[^a-zA-Z]'|[^a-zA-Z'])+"

  /**
    * Convert an input DataFrame into an RDD of word bags
    *
    * @param config A dictionary configuration object that specifies some aspects of how word conversion takes place
    * @param idColumn The column of the input DataFrame containing a unique record ID for each row
    * @param documentColumn The column of the input DataFrame containing the documents to be analyzed
    * @param input The input DataFrame
    * @return An RDD of word bags, indexed by record ID
    */
  def dataframeToWordBags (config: DictionaryConfig,
                           idColumn: String,
                           documentColumn: String)
                          (input: DataFrame): RDD[(Any, Map[String, Int])] = {
    val stopWords = getWordFile(config.stopwords, config.caseSensitive)
    val goWords = getWordFile(config.dictionary, config.caseSensitive)
    input.
      // Pull out our document column
      select(idColumn, documentColumn).rdd.map(row => (row.get(0), row.getString(1).trim)).
      // Get rid of null documents
      filter(!_._2.isEmpty).
      // Change simple document strings to word bags
      map { case (id, document) => (id, documentToWordBag(document, config.caseSensitive, stopWords, goWords)) }
  }


  /**
    * Convert an input dataset that contains texts into an output dataset that contains word bags
 *
    * @param config A dictionary configuration that indicates how dictionaries are to be formed.  This is currently
    *               only used to determine case sensitivity, but could be used for more in the future.
    * @param textExtractorFcn A function to extract the text from an input data record
    * @param wordBagInjectorFcn A function to inject the word bag into an input data record to create an output data
    *                           record
    * @param input The input data
    * @tparam T The input data type
    * @tparam U The output data type
    * @return A dataset containing the created word bags
    */
  def textToWordBags[T, U: ClassTag] (config: DictionaryConfig,
                                      textExtractorFcn: T => String,
                                      wordBagInjectorFcn: (T,   Map[String, Int]) => U)(input: RDD[T]): RDD[U] = {
    input.map { t =>
      val words = MutableMap[String, Int]()

      textExtractorFcn(t).split(notWord).foreach { word =>
        val casedWord = if (config.caseSensitive) {
          word
        } else {
          word.toLowerCase
        }
        words(casedWord) = words.getOrElse(casedWord, 0) + 1
      }

      wordBagInjectorFcn(t, words.toMap)
    }
  }

  /**
    * Convert an RDD of T to an RDD of word bags
    *
    * @param config A dictionary configuration object that specifies some aspects of how word conversion takes place
    * @param idExtractorFcn A function to extract the ID from each record
    * @param documentExtractorFcn A function to extract a document from each record
    * @param input The input record collection
    * @tparam I The ID type of each record
    * @tparam T The type of input record
    * @return An RDD of word bags, indexed by record ID
    */
  def rddToWordBags[I, T] (config: DictionaryConfig,
                           idExtractorFcn: T => I,
                           documentExtractorFcn: T => String)
                          (input: RDD[T]): RDD[(I, Map[String, Int])] = {
    val stopWords = getWordFile(config.stopwords, config.caseSensitive)
    val goWords = getWordFile(config.dictionary, config.caseSensitive)
    input.
      // Pull out our document column
      map(t => (idExtractorFcn(t), documentExtractorFcn(t).trim)).
      // Get rid of null documents
      filter(!_._2.isEmpty).
      // Change simple document strings to word bags
      map { case (id, document) => (id, documentToWordBag(document, config.caseSensitive, stopWords, goWords)) }
  }


  /**
    * Take an RDD of (id, word bag) pairs, and transform the word bags into vector references into a common dictionary,
    * with the entries being the count of words in each word bag.
    *
    * @param dictionary A dictionary of all words; the key is the word, the value, the place in the index
    * @param input A dataset of (id, word bag)
    * @tparam T The type of the ID associated with each word bag
    * @return a dataset of (id, vector), where the vectors reference the words in the dictionary
    */
  def wordBagToWordVector[T] (dictionary: Map[String, Int])(input: RDD[(T, Map[String, Int])]): RDD[(T, Vector)] = {
    // Use our dictionary to map word maps into sparse vectors by map index
    val wordVectors = input.map { case (id, wordBag) =>
      val indicesAndValues = wordBag.flatMap { case (word, count) =>
        dictionary.get(word).map(wordIndex => (wordIndex, count))
      }.toArray.sortBy(_._1)
      val wordVector: Vector = new SparseVector(dictionary.size, indicesAndValues.map(_._1), indicesAndValues.map(_._2.toDouble))
      (id, wordVector)
    }
    wordVectors
  }


  /**
    * Create a dictionary of the terms seen in a set of documents, along with the term frequency (the number of
    * documents in which it occurs) for each term
    *
    * @param config A description of which words are to be chosen
    * @param wordBagExtractorFcn A function to pull a word bag from each input record
    * @param wordBags The input data, already processed into word bags by dataFrameToWordBags or rddToWordBags
    * @return The dictionary to use with this set of word bags
    */
  def getDictionary[T] (config: DictionaryConfig,
                        wordBagExtractorFcn: T => Map[String, Int])
                       (wordBags: RDD[T]): Array[(String, Int)] = {
    val docCount = config.needDocCount.map(yes => wordBags.count)
    val minDF = config.minDF.map(_ * docCount.getOrElse(0L))
    val maxDF = config.maxDF.map(_ * docCount.getOrElse(0L))

    val dictionaryRDD = wordBags.map(wordBagExtractorFcn).
      // ... Flatten out word bags, adding in document count
      flatMap(_.map { case (word, count) => (word, 1) }).
      // ... total document counts
      reduceByKey(_ + _).
      // ... weed out words that appear too infrequently or too often
      filter {
      case (word, documentCount) =>
        (minDF.map(_ <= documentCount).getOrElse(true) && maxDF.map(_ >= documentCount).getOrElse(true))
    }

    config.maxFeatures.map { max =>
      // We've declared a maximum number of words, take the top N words
      dictionaryRDD.sortBy(-_._2).take(max)
    }.getOrElse {
      // No word maximum; take them all.
      dictionaryRDD.collect
    }.sortWith(_._2 > _._2)
  }


  /**
    * Create a series of dictionaries of the terms seen in a set of documents, along with the term frequency (the
    * number of documents in which it occurs) for each term.
    *
    * This should be used (as opposed to the above getDictionary) when the document dataset contains multiple
    * theoretical datasets, each of which should be examined separately - for instance, a tile set where each level
    * of tiles is considered on its own.
    *
    * @param config A description of which words are to be chosen
    * @param wordBagExtractorFcn A function to pull a dictionary index and a document (in the form of a word bag) from
    *                            each input record,  All documents with the same dictionary index will contribute to
    *                            the same dictionary.
    * @param input The input data, already processed into word bags by dataFrameToWordBags or rddToWordBags
    * @tparam T The type of input record
    * @return The dictionaries to use with this set of word bags
    */
  def getDictionaries[T] (config: DictionaryConfig,
                          wordBagExtractorFcn: T => (Int, Map[String, Int]))
                         (input: RDD[T]): Array[(String, Map[Int, Int])] = {
    val indexedDocuments = input.map(wordBagExtractorFcn)

    val docCounts = config.needDocCount.map { yes =>
      indexedDocuments.map { case (docIndex, doc) => (docIndex, 1) }.reduceByKey(_ + _).collect.toMap
    }
    val minDF = config.minDF.map(min => docCounts.get.map { case (docIndex, docCount) => (docIndex, docCount * min) })
    val maxDF = config.maxDF.map(max => docCounts.get.map { case (docIndex, docCount) => (docIndex, docCount * max) })
    val limitToRange: Map[Int, Int] => ((Int, Int)) => Option[(Int, Int)] = rangeLimitation(minDF, maxDF)(_)

    val dictionaryRDD = indexedDocuments.flatMap { case (docIndex, docWords) =>
      // ... Flatten out word bags, adding in document count
      docWords.map { case (word, count) => (word, MutableMap(docIndex -> 1)) }
    }.reduceByKey { (a, b) =>
      // ... total document counts, by document index, in place.
      b.foreach { case (docIndex, count) => a(docIndex) = a.getOrElse(docIndex, 0) + count }
      a
    }.map { case (term, documentCounts) =>
      // ... weed out terms that appear too frequently or infrequently
      docCounts.map { dc =>
        (term, documentCounts.flatMap(limitToRange(dc)(_)).toMap)
      }.getOrElse((term, documentCounts.toMap))
    }.filter(_._2.size > 0)

    config.maxFeatures.map { maxFeatures =>
      dictionaryRDD.sortBy(-_._2.values.max).take(maxFeatures)
    }.getOrElse(
      dictionaryRDD.collect
    ).sortBy(_._1)
  }


  /**
    * Convert an RDD of word bags into an RDD of word vectors (i.e., eliminate references to the strings, make it
    * all numeric).  Word vectors are still stored sparsely (i.e., as maps)
    *
    * @param data The collection of word bags (word, term frequency) to convert
    * @param dictionary The dictionary to use to convert them.  Dictionary form is (word, document frequency)
    * @return A new collection of word bags in the form of (index within dictionary, term frequency)
    */
  def wordBagToWordVector (data: RDD[(Any, Map[String, Int])],
                           dictionary: Array[(String, Int)]): RDD[(Any, Map[Int, Int])] = {
    val dictionaryLookup = dictionary.map(_._1).zipWithIndex.toMap
    data.map { case (id, wordBag) =>
      (id, wordBag.flatMap { case (word, termFrequency) =>
        dictionaryLookup.get(word).map(index => (index, termFrequency))
      })
    }
  }


  // Helper function for getDictionaries.  This limits values to indexed minima and maxima - so if the index of the
  // value is n, the value has to lie between minDF(n) and maxDF(n)
  // Basically, this is only outside getDictionaries to reduce it's complexity so it passes scalastyle tests.  That
  // being said this does make sense as a separate function, so does actually reduce complexity, so I guess scalastyle
  // works... sort-of.
  private def rangeLimitation (minDF: Option[Map[Int, Double]], maxDF: Option[Map[Int, Double]])
                              (documentCounts: Map[Int, Int])
                              (docData: (Int, Int)): Option[(Int, Int)] = {
    val (docIndex, termCount) = docData

    if (
      minDF.map(_ (docIndex) <= documentCounts(docIndex)).getOrElse(true) &&
        maxDF.map(_ (docIndex) >= documentCounts(docIndex)).getOrElse(true)
    ) {
      Some((docIndex, termCount))
    } else {
      None
    }
  }


  private def getWordFile (fileNameOpt: Option[String], caseSensitive: Boolean): Option[Set[String]] = {
    fileNameOpt.map { fileName =>
      val fileStream =
        Option(getClass.getResourceAsStream(fileName))
          .getOrElse(new FileInputStream(fileName))

      val rawWords = scala.io.Source.fromInputStream(fileStream).getLines.map(_.trim)
      if (caseSensitive) {
        rawWords.toSet
      } else {
        rawWords.map(_.toLowerCase).toSet
      }
    }
  }


  private def documentToWordBag (document: String,
                                 caseSensitive: Boolean,
                                 stopWords: Option[Set[String]],
                                 goWords: Option[Set[String]]): Map[String, Int] = {
    val wordCounts = MutableMap[String, Int]()
    document.split(notWord).foreach { uncasedWord =>
      val word =
        if (caseSensitive) {
          uncasedWord.trim
        } else {
          uncasedWord.trim.toLowerCase
        }
      if (
        stopWords.map(!_.contains(word)).getOrElse(true) &&
          goWords.map(_.contains(word)).getOrElse(true)
      ) {
        wordCounts(word) = wordCounts.getOrElse(word, 0) + 1
      }
    }

    wordCounts.toMap
  }
}
