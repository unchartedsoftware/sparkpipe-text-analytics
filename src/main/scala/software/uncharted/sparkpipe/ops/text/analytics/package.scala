/**
 * Copyright © 2013-2017 Uncharted Software Inc.
 *
 * Property of Uncharted™, formerly Oculus Info Inc.
 *
 * http://uncharted.software/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package software.uncharted.sparkpipe.ops.text

import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.{DistributedLDAModel, LDA, LDAModel, LocalLDAModel}
import org.apache.spark.mllib.linalg.{DenseVector, Matrix, SparseVector, Vector}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions
import software.uncharted.sparkpipe.Pipe
import software.uncharted.sparkpipe.ops.core.dataframe.text
import software.uncharted.sparkpipe.ops.core.dataframe

import scala.collection.mutable
import scala.math.Ordering

/**
  * TFIDF and LDA text analysis operations.
  *
  * @author nkronenfeld, cbethune
  */
package object analytics {

  private[analytics] case class WordScore (word: String, score: Double)
  private[analytics] case class TopicScore (topic: Seq[WordScore], score: Double)
  private[analytics] case class DocumentTopics (ldaDocumentIndex: Long, topics: Seq[TopicScore])

  private[analytics] case class TFIDFInfo(id: Long, scores: Seq[(String, Double)])
  private[analytics] case class TermInfo(id: Long, words: Set[String], termFrequencies: mutable.Map[String, Int], maxFrequency: Int)

  private val tmpDir: String = "/tmp"

  /**
    * Perform TDFIF analysis on a column of text documents in a dataframe.
    *
    * @param textCol The name of the column containing the text to analyze
    * @param tfidfCol The name of the column to store the results in
    * @param tfType The term frequency computation type
    * @param idfType The inverse document frequency computation type
    * @param minTF The minimum number of times a word must occur in a document to be
    *              included in that document's term frequency list
    * @param minDF The minimum number of times a word must occur in the document corpus for
    *              it to be included in the corpus frequency list
    * @param retainedTerms The number of terms to keep based on computed TFIDF score
    * @param input The dataframe containing the input to analyze
    * @return The original dataframe augmented the computed TFIDF score.
    */
  def textTFIDF (textCol: String,
                    tfidfCol: String,
                    tfType: TFType = RawTF,
                    idfType: IDFType = LogIDF,
                    minTF: Int = 0,
                    minDF: Int = 0,
                    retainedTerms: Option[Int] = None)
  (input: DataFrame): DataFrame = {
    val splitTextCol = "__split_text__"
    Pipe(input)
      .to(dataframe.cache)
      .to(dataframe.addColumn(splitTextCol, (s: String) => s.split(transformations.notWord), textCol))
      .to(wordBagTFIDF(splitTextCol, tfidfCol, tfType, idfType, minTF, minDF, retainedTerms))
      .to(_.drop(splitTextCol))
      .run()
  }

  /**
    * Perform TFIDF analysis on a column of word bags in a dataframe.
    *
    * @param textCol The name of the column containing the text word bags to analyze
    * @param tfidfCol The name of the column to store the results in
    * @param tfType The term frequency computation type
    * @param idfType The inverse document frequency computation type
    * @param minTF The minimum number of times a word must occur in a document to be
    *              included in that document's term frequency list
    * @param minDF The minimum number of times a word must occur in the document corpus for
    *              it to be included in the corpus frequency list
    * @param retainedTerms The number of terms to keep based on computed TFIDF score
    * @param input The dataframe containing the input to analyze
    * @return The original dataframe augmented the computed TFIDF score
    */
  // scalastyle:off method.length
  def wordBagTFIDF (textCol: String,
                 tfidfCol: String,
                 tfType: TFType = RawTF,
                 idfType: IDFType = LogIDF,
                 minTF: Int = 0,
                 minDF: Int = 0,
                 retainedTerms: Option[Int] = None)
                (input: DataFrame): DataFrame = {

    val idCol = "__id__"

    // Filter down to the text column and add a unique row id to use for a final join
    val idInput = Pipe(input)
      .to(_.withColumn(idCol, functions.monotonically_increasing_id()))
      .to(dataframe.cache)
      .run

    val textInput = Pipe(idInput).to(_.select(textCol, idCol)).run

    // Compute word counts for the entire corpus, filter by called supplied min DF and broadcast it
    val documentFrequencies = Pipe(textInput)
      .to(text.uniqueTerms(textCol))
      .to(_.retain((k, v) => v >= minDF)) // happening in driver - could be mem issue for large dict
      .run
    val broadcastDF = input.sparkSession.sparkContext.broadcast(documentFrequencies)

    // compute the word frequencies for the terms in each row text field
    val rdd = textInput.rdd
    val termFrequencies = rdd.map { row =>
      val termMap = mutable.Map[String, Int]() // using mutable because immutable in tight loops causes a lot of GC
      val words = row.getSeq[String](0)
      words.foreach { word =>
        val lowerWord = word.toLowerCase
        termMap(lowerWord) = termMap.getOrElse(lowerWord, 0) + 1
      }
      // filter out any that are below the minTF
      termMap.retain((k, v) => v >= minTF)

      // store values needed for subsequent TF computation
      val maxFrequency = termMap.values.fold(0)((c, v) => Math.max(c, v))
      TermInfo(row.getLong(1), words.toSet, termMap, maxFrequency)
    }

    // apply the caller supplied TF and IDF functions to the raw counts and combine for final scores
    val tfidf = termFrequencies.map { termInfo =>
      val tfidfMap = mutable.SortedSet[(String, Double)]()(Ordering[(Double, String)].on(t => (t._2, t._1)))

      termInfo.words.foreach { word =>
        // proceed with the calculation if words made it past min count filters
        if (termInfo.termFrequencies.contains(word) && broadcastDF.value.contains(word)) {
          val tf = tfType.termFrequency(termInfo.termFrequencies(word), termInfo.words.size, termInfo.maxFrequency)
          val idf = idfType.inverseDocumentFrequency(broadcastDF.value.size, broadcastDF.value(word))
          tfidfMap.add((word, tf * idf))
        }
      }

      // Extract the requested number of retained terms
      TFIDFInfo(termInfo.id, retainedTerms.map(tfidfMap.takeRight).getOrElse(tfidfMap).toSeq)
    }

    // Join the computed word scores to the original data by the ID and return
    val resultDf = input.sparkSession.createDataFrame(tfidf).toDF(idCol, tfidfCol)
    resultDf.join(idInput, idCol).drop(idCol)
  }


  /**
    * Perform LDA analysis on documents in a dataframe.
    *
    * @param textCol The name of the column containing the text to analyze
    * @param dictionaryConfig The dictionary creation configuration
    * @param ldaConfig LDA job configuration
    * @param input The dataframe containing the data to analyze
    * @return A dataframe containing the input data, augmented with the topics found in that input data
    */
  def textLDA (textCol: String, dictionaryConfig: DictionaryConfig, ldaConfig: LDAConfig)
              (input: DataFrame): DataFrame = {
    val idCol = "__id__"
    val sqlc = input.sqlContext

    val indexedInput = input.withColumn(idCol, functions.monotonically_increasing_id)

    // Mutate our input into indexed word bags
    val inputWords = transformations.textToWordBags[Row, (Long, Map[String, Int])](
      dictionaryConfig,
      _.getString(1),
      (row, wordBag) => (row.getLong(0), wordBag)
    )(indexedInput.select(idCol, textCol).rdd)

    // Create our dictionary from the set of input words
    val dictionary = transformations.getDictionary[(Long, Map[String, Int])](dictionaryConfig, _._2)(inputWords)
      .zipWithIndex.map { case ((word, count), index) => (word, index)}.toMap

    // Perform our LDA analysis
    val rawResults = wordBagLDATopics(ldaConfig, dictionary, inputWords)

    // Mutate to dataframe form for joining with original data
    val dfResults = sqlc.createDataFrame(rawResults.map{case (index, scores) => DocumentTopics(index, scores)})

    // Join our LDA results back with our original data and drop working columns
    indexedInput.join(dfResults, indexedInput(idCol) === dfResults("ldaDocumentIndex"))
      .drop("ldaDocumentIndex")
      .drop(idCol)
  }

  /**
    * Perform LDA on a dataset of documents in an RDD.  Callers must supply a function to
    * extract the documents from the RDD for processing.
    *
    * @param dictionaryConfig The dictionary creation configuration
    * @param ldaConfig LDA job configuration
    * @param docExtractor A function to extract the document to be analyzed from each input record
    * @param input An RDD containing the data to analyze
    * @tparam T The input data type
    * @return An RDD containing the input data, augmented with the topics found in that input data
    */
  def textLDA[T] (dictionaryConfig: DictionaryConfig, ldaConfig: LDAConfig, docExtractor: T => String)
                 (input: RDD[T]): RDD[(T, Seq[TopicScore])] = {
    val indexedInput = input.map(t => (t, docExtractor(t))).zipWithIndex().map(_.swap)

    // Mutate our input into indexed word bags
    val inputWords = transformations.textToWordBags[(Long, (T, String)), (Long, (T, Map[String, Int]))](
      dictionaryConfig,
      _._2._2,
      (original, wordBag) => (original._1, (original._2._1, wordBag))
    )(indexedInput)

    // Create our dictionary from the set of input words
    val dictionary = transformations.getDictionary[(Long, (T, Map[String, Int]))](dictionaryConfig, _._2._2)(inputWords)
      .zipWithIndex.map { case ((word, count), index) => (word, index)}.toMap

    // Perform our LDA analysis
    val ldaResults = wordBagLDATopics(ldaConfig, dictionary, inputWords.map { case (index, (original, words)) => (index, words) })

    // Join our LDA results back with our original data
    indexedInput.join(ldaResults).map { case (index, ((original, words), scores)) =>
      (original, scores)
    }
  }

  /**
    * Perform LDA on a dataset of indexed documents in an RDD.  Callers must supply functions to extract
    * the documents and their IDs from the input RDD.
    *
    * @param dictionaryConfig The dictionary creation configuration
    * @param ldaConfig LDA job configuration
    * @param docExtractor A function to extract the document to be analyzed from each input record
    * @param idExtractor A function to extract the document index from each input record.  Each record should have a
    *                    unique document index.
    * @param input An RDD containing the data to analyze
    * @tparam T The input data type
    * @return An RDD of (ID, scored topics) tuples
    */
  def textLDATopics[T] (dictionaryConfig: DictionaryConfig, ldaConfig:LDAConfig, docExtractor: T => String, idExtractor: T => Long)
                       (input: RDD[T]): RDD[(Long, Seq[TopicScore])] = {
    val indexedDocuments = input.map(t => (idExtractor(t), docExtractor(t)))

    // Mutate our input into indexed word bags
    val inputWords = transformations.textToWordBags[(Long, String), (Long, Map[String, Int])](
      dictionaryConfig,
      _._2,
      (original, wordBag) => (original._1, wordBag)
    )(indexedDocuments)

    // Create our dictionary from the set of input words
    val dictionary = transformations.getDictionary[(Long, Map[String, Int])](dictionaryConfig, _._2)(inputWords)
      .zipWithIndex.map { case ((word, count), index) => (word, index)}.toMap

    // Perform our LDA analysis
    wordBagLDATopics(ldaConfig, dictionary, inputWords)
  }

  /**
    * Perform LDA on an a set of word bags in an RDD.  Callers must supply a function to
    * extract the word bags from the RDD for processing.
    *
    * * @param dictionaryConfig The dictionary creation configuration
    *
    * @param ldaConfig LDA job configuration
    * @param wordBagExtractor A function to extract the word bags to be analyzed from each input record
    * @param input An RDD containing the data to analyze
    * @tparam T The input data type
    * @return An RDD containing the input data, augmented with the topics found in that input data
    *
    */
  def wordBagLDA[T] (dictionaryConfig: DictionaryConfig, ldaConfig: LDAConfig, wordBagExtractor: T => Map[String, Int])
                    (input: RDD[T]): RDD[(T, Seq[TopicScore])] = {
    val dictionary = transformations.getDictionary(dictionaryConfig, wordBagExtractor)(input)
      .zipWithIndex.map { case ((word, count), index) => (word, index)}.toMap

    val wordBagsWithIds = input.map(t => (t, wordBagExtractor(t))).zipWithIndex().map(_.swap)
    val ldaResults = wordBagLDATopics(ldaConfig, dictionary, wordBagsWithIds.map { case (key, (in, wordBag)) => (key, wordBag) })

    wordBagsWithIds.join(ldaResults).map { case (key, ((in, wordBag), scores)) =>
      (in, scores)
    }
  }

  /**
    * Perform LDA on a dataset of indexed word bags in an RDD.
    *
    * @param config LDA job configuration
    * @param dictionary A dictionary of words to consider in our documents
    * @param input An RDD of indexed word bags; the Long id field should be unique for each row.
    * @return An RDD of the same word bags, with a sequence of topics attached.  The third, attached, entry in each
    *         row should be read as Seq[(topic, topicScoreForDocument)], where the topic is
    *         Seq[(word, wordScoreForTopic)]
    */
  def wordBagLDATopics (config: LDAConfig, dictionary: Map[String, Int], input: RDD[(Long, Map[String, Int])]): RDD[(Long, Seq[TopicScore])] = {
    val documents = transformations.wordBagToWordVector(dictionary)(input)
    lda(config, dictionary, documents)
  }

  private def lda (config: LDAConfig,
                   dictionary: Map[String, Int],
                   documents: RDD[(Long, Vector)]): RDD[(Long, Seq[TopicScore])] = {
    val sc = documents.context

    val ldaEngine = new LDA()
      .setK(config.numTopics)
      .setOptimizer("em")
    config.chkptInterval.map(r => ldaEngine.setCheckpointInterval(r))
    config.maxIterations.map(r => ldaEngine.setMaxIterations(r))

    val model = getDistributedModel(sc, ldaEngine.run(documents))

    // Unwind the topics matrix using our dictionary (but reversed)
    val allTopics = getTopics(model, dictionary, config.wordsPerTopic)

    // Doc id, topics, weights
    val topicsByDocument: RDD[(Long, Array[Int], Array[Double])] = model.topTopicsPerDocument(config.topicsPerDocument)

    // Unwind the topics for each document
    topicsByDocument.map { case (docId, topics, weights) =>
      // Get the top n topic indices
      val topTopics = topics.zip(weights).sortBy(-_._2).take(config.topicsPerDocument).toSeq

      // Expand these into their topic word vectors
      (docId, topTopics.map { case (index, score) =>
        TopicScore(allTopics(index), score)
      })
    }
  }

  private def getDistributedModel (sc: SparkContext, model: LDAModel): DistributedLDAModel = {
    model match {
      case distrModel: DistributedLDAModel => distrModel
      case localModel: LocalLDAModel =>
        localModel.save(sc, tmpDir + "lda")
        DistributedLDAModel.load(sc, tmpDir + "lda")
    }
  }

  private def getTopics (model: DistributedLDAModel, dictionary: Map[String, Int], wordsPerTopic: Int): Map[Int, Seq[WordScore]] = {
    val topics: Matrix = model.topicsMatrix
    val reverseDictionary = dictionary.map(_.swap)

    (0 until topics.numCols).map(c => (c, util.MatrixUtilities.column(topics, c))).map { case (topicIndex, topicVector) =>
      val wordScores = (topicVector match {
        case v: DenseVector =>
          v.values.zipWithIndex.map { case (value, index) =>
            WordScore(reverseDictionary(index), value)
          }
        case v: SparseVector =>
          v.indices.map(reverseDictionary(_)).zip(v.values).map{case (word, score) => WordScore(word, score)}
      }).sortBy(-_.score).take(wordsPerTopic).toSeq

      (topicIndex, wordScores)
    }.toMap
  }
}
