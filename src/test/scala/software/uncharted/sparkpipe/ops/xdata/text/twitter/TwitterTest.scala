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

package software.uncharted.sparkpipe.ops.xdata.text.twitter

import java.text.SimpleDateFormat

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DoubleType, StringType, StructField, StructType}
import software.uncharted.sparkpipe.Pipe
import software.uncharted.sparkpipe.ops.xdata.text.twitter.util._
import software.uncharted.sparkpipe.ops.xdata.text.util.RangeDescription
import software.uncharted.sparkpipe.spark.SparkFunSpec

class TwitterTest extends SparkFunSpec {
  val path = classOf[TwitterTest].getResource("/topic-modelling/topic-modelling.conf").toURI.getPath

  // Run the test from path/to/xdata-pipeline-ops/
  val oldDir = System.getProperty("user.dir")
  val newDir = path.substring(0, path.indexOf("xdata-pipeline-ops") + 18)
  System.setProperty("user.dir", newDir)

  val stopWords = WordDict.loadStopwords(Array(
    "src/main/resources/stopwords/stopwords_all_en.v2.txt",
    "src/main/resources/stopwords/stopwords_ar.txt",
    "src/main/resources/stopwords/stopwords_html_tags.txt"
  ).toList)

  describe("#getDocumentTopic") {
    try {
      it("should label tweets with topics") {
        //Basic test to make sure overall process runs.
        val rddData = sc.parallelize(Seq(
          Tweet("1", "Sat Oct 01 00:00:10 +0000 2016", "The TPP needs to be killed!"),
          Tweet("2", "Sat Oct 01 02:00:10 +0000 2016", "Death to all!!"),
          Tweet("3", "Sat Oct 01 04:00:10 +0000 2016", "ISIS is death"),
          Tweet("4", "Sun Oct 02 00:00:10 +0000 2016", "i wish i had been independent and wise because this is silly"),
          Tweet("5", "Sun Oct 02 00:00:10 +0000 2016", "this is the final test i did it. Now, more than ever, we need Trump.")
        ))
        val stopwords_bcst = sc.broadcast(stopWords)

        val dfData = sparkSession.createDataFrame(rddData)

        val formatter = new SimpleDateFormat("yyyy-MM-dd")
        val minTime = formatter.parse("2016-09-30").getTime
        val maxTime = formatter.parse("2016-10-03").getTime
        val timeRange: RangeDescription[Long] = RangeDescription.fromStep(minTime, maxTime, 24 * 60 * 60 * 1000L)

        val result = getDocumentTopic(
          "date",
          "id",
          "text",
          "topic",
          Some(1 / Math.E),
          Some(0.01),
          timeRange,
          Some(10),
          Some(2),
          Some(10),
          stopwords_bcst,
          None)(dfData)

        //Every row should be returned.
        assert(result.count() === 5)

        //The output field should exist.
        assert(result.schema.fieldIndex("topic") > 0)
      }

      it("should run a complete corpus and return tweets labelled with topics") {
        //Run a bigger test and make sure the returned topics are generally accurate.
        //You will never be able to get 100% identical result, but the general content of the returned topics
        //will be consistent between runs.
        val reader_corpus = sparkSession.read
          .format("com.databricks.spark.csv")
          .option("header", "true")
          .option("inferSchema", "true")
          .option("delimiter", "\t")
        val corpus = Pipe(reader_corpus.load("src/test/resources/topic-modelling/isil_keywords.20160901-000000.txt")).run().filter("C7 = 'en'")
        val stopwords_bcst = sparkSession.sparkContext.broadcast(stopWords)

        val formatter = new SimpleDateFormat("yyyy-MM-dd")
        val minTime = formatter.parse("2016-08-30").getTime
        val maxTime = formatter.parse("2016-09-03").getTime
        val timeRange = RangeDescription.fromStep(minTime, maxTime, 24 * 60 * 60 * 1000).asInstanceOf[RangeDescription[Long]]

        val result = getDocumentTopic(
          "date",
          "id",
          "text",
          "topic",
          Some(1 / Math.E),
          Some(0.01),
          timeRange,
          Some(10),
          Some(5),
          Some(3),
          stopwords_bcst,
          None)(corpus)

        //Every row should be returned.
        assert(result.count() === corpus.count())

        //The output field should exist.
        assert(result.schema.fieldIndex("topic") > 0)

        val resultC = result.select("text", "topic").collect()

        //Group by topic. isis, joke, pewdiepie, youtube & twitter should make up the top 5.
        val expectedTopics = Array("isis", "joke", "twitter", "youtube", "pewdiepie")
        val topicCounts = resultC.flatMap(r => r(1).asInstanceOf[Map[String, Double]])
          .groupBy(t => t._1)
          .map(g => (g._1, g._2.length))

        val unexpectedCount = topicCounts.toArray.sortBy(tc => -tc._2).take(5)
          .count(x => !expectedTopics.contains(x._1))
        assert(unexpectedCount === 0)
      }
    } finally {
      System.setProperty("user.dir", oldDir)
    }
  }

  describe("#removeReTweets") {
    it("should remove duplicate tweets") {
      val rddData = sc.parallelize(Seq(
        Tweet("1", "Sat Oct 01 00:00:10 +0000 2016", "This is a bloody good tweet"),
        Tweet("2", "Sat Oct 01 02:00:10 +0000 2016", "@foxnews This is a bloody good tweet"),
        Tweet("3", "Sat Oct 01 04:00:10 +0000 2016", "This is a bloody good tweet"),
        Tweet("4", "Sun Oct 02 00:00:10 +0000 2016", "Another tweet for good measure"),
        Tweet("5", "Sun Oct 02 00:00:10 +0000 2016", "@Me This is a bloody good tweet.")
      ))
      val dfData = sparkSession.createDataFrame(rddData)

      val unique = removeReTweets("text")(dfData)
      assert(unique.count() === 2)
    }
  }

  describe("#BDPParallel") {
    it("should split RDD of string array of size 3") {
      val data1 = Array("great", "jazz", "music")
      val data2 = Array("that cat", "was", "cool")
      val rddData = sc.parallelize(Seq(data1, data2))

      val result = BDPParallel.keyvalueRDD(rddData).collect()
      val expected = Array(("great",("jazz","music")), ("that cat",("was","cool")))

      assertResult(expected)(result)
    }
  }

  describe("BDP") {
    it ("should initialize a BDP instance's tfidf dictionary/map with the input map.") {
      val bdp = new BDP(3)

      val sampleTFIDF = Array(("2017-0101", "festival", 2.0), ("2017-0105", "music", 3.7), ("2017-0101", "programming", 5.0))
      val tfidf_bcst = sc.broadcast(sampleTFIDF)
      val word_dict = Map("programming"->1)
      bdp.initTfidf(tfidf_bcst, "2017-0101", word_dict)

      val result = bdp.tfidf_dict
      val expected = Map(1->5.0)
      assertResult(expected)(result)

    }
  }

  describe("#TwitterTokenizer") {
    import TwitterTokenizer._
    it("should test normalizeEmoji") {
      val textEmoji = "Today is a sunny day ☺" // scalastyle:ignore
      val repeatEmoji = "Today is a sunny day ☺☺"
      val textSpace = "      hello there   "
      val resultEmoji = normalizeEmoji(textEmoji)
      val resultRepeat = normalizeEmoji(repeatEmoji)
      val resultSpace = normalizeEmoji(textSpace)
      val expected = "Today is a sunny day  <HAPPY_FACE>"

      assertResult(expected)(resultEmoji)
      assertResult(expected)(resultRepeat)
      assertResult("hello there")(resultSpace)
    }

    it("should test replaceEmoji") {
      val textEmoji = "Today is a sunny day ☺"
      val repeatEmoji = "Today is a sunny day ☺☺"
      val textEmoji2 = "Today is a sunny☺ day"
      val resultEmoji = replaceEmoji(textEmoji)
      val resultRepeat = replaceEmoji(repeatEmoji)
      val resultEmoji2 = replaceEmoji(textEmoji2)
      val expected = "Today is a sunny day"

      assertResult(expected)(resultEmoji)
      assertResult(expected)(resultRepeat)
      assertResult("Today is a sunny  day")(resultEmoji2)

    }

    it("should test normalizeEmoticons") {
      val textEmoji = "Today is a sunny day ☺"
      val textSpace = "      hello there   "
      val textEmoticon = "hello there:P"
      val resultEmoji = normalizeEmoticons(textEmoji)
      val resultSpace = normalizeEmoticons(textSpace)
      val resultEmoticon = normalizeEmoticons(textEmoticon)
      val expected = "hello there"

      assertResult("Today is a sunny day ☺")(resultEmoji)
      assertResult(s"$expected")(resultSpace)
      assertResult(s"$expected <LOL>")(resultEmoticon)
    }

    it("tokenizeCleanText") {
      val text = "everyday is  awesome    "
      val result = tokenizeCleanText(text)
      val expected = Array("everyday", "is", "awesome")
      assertResult(expected)(result)
    }

    it("should tokenize a sample text") {
      val text = "hello:)there world☺today is a, sunny day"
      val result = tokenize(text)
      val expected = Array("hello", "there", "world", "today", "is", "a", "sunny", "day")
      assertResult(expected)(result)
    }

    it ("should normalize sample texts") {
      //return empty string since text contains lang listed in notNeededLangs
      val text = "this word means sunrise: 日出"
      val result = normalize(text)
      val expected = ""
      assertResult(expected)(result)

      //normalize emoticons and emojis instead of replacing them
      val text2 = "The weather this weekend is good ☺ I think I will go biking:P"
      val result2 = normalize(text2, false)
      val expected2 = "The weather this weekend is good  <HAPPY_FACE>  I think I will go biking <LOL>"
      assertResult(expected2)(result2)
    }
  }

  describe("#TextUtil") {
    it("should test cleanStopwords ") {
      val result = TextUtil.cleanStopwords("Sometimes I believe in as many as six impossible things before breakfast. That is an excellent practice.",stopWords)
      val expected = "impossible breakfast excellent practice"
      assertResult(expected)(result)
    }
  }

  describe("#SampleRecorder") {
    it ("should set a TFIDF dictionary and get the weight of its words") {
      val words = Array("engineering", "science", "art")
      val m = words.length
      val k = 3
      val weighted = true
      val SR = new SampleRecorder(m, k, weighted)
      val sample_tfidf = Map(1->2.0, 3->1.0)
      SR.setTfidf(sample_tfidf)

      assertResult(sample_tfidf)(SR.tfidf_dict)
      assertResult(2.0)(SR.getWeight(1, 0.1))
      assertResult(1.0)(SR.getWeight(3, 0.1))
    }
  }

  describe("#getDocumentTopicRawTFIDF") {
    it("should perform tfidf processing on a given corpus") {
      try {
        val corpusRDD = sc.parallelize(Seq(
          Tweet("1", "Wed Apr 12 00:00:10 +0000 2017", "Cats are the best pets"),
          Tweet("2", "Sun Apr 16 00:00:10 +0000 2017", "Happy national pets day"),
          Tweet("3", "Mon Apr 17 00:00:10 +0000 2017", "Cats are fluffy")
        ))
        val corpusDF = sparkSession.createDataFrame(corpusRDD)

        val formatter = new SimpleDateFormat("yyyy-MM-dd")
        val minTime = formatter.parse("2017-04-01").getTime
        val maxTime = formatter.parse("2017-10-03").getTime
        val timeRange: RangeDescription[Long] = RangeDescription.fromStep(minTime, maxTime, 24 * 60 * 60 * 1000L)

        val stopwords_bcst = sc.broadcast(stopWords)

        val scoresSeq = Seq(Row("2017-04-12","Cats", 0.287682072),
                            Row("2017-04-12", "are", 0.287682072),
                            Row("2017-04-12", "the", 0.693147181),
                            Row("2017-04-12", "best", 0.693147181),
                            Row("2017-04-12", "pets", 0.287682072),
                            Row("2017-04-16", "Happy", 0.693147181),
                            Row("2017-04-16", "national", 0.693147181),
                            Row("2017-04-16", "pets", 0.287682072),
                            Row("2017-04-16", "day", 0.693147181),
                            Row("2017-04-17", "Cats", 0.287682072),
                            Row("2017-04-17", "are", 0.287682072),
                            Row("2017-04-17", "fluffy", 0.693147181))
        val rddScores = sc.parallelize(scoresSeq)
        val schemaTFIDF = StructType(Seq(
          StructField("date", StringType),
          StructField("term", StringType),
          StructField("score", DoubleType)))
        val tfidfScores = sparkSession.createDataFrame(rddScores, schemaTFIDF)

        val result = getDocumentTopicRawTFIDF(
          "date",
          "id",
          "text",
          "topic",
          Some(1 / Math.E),
          Some(0.01),
          timeRange,
          Some(10),
          Some(2),
          Some(10),
          stopwords_bcst
        )(corpusDF, tfidfScores)

        //Every row should be returned.
        assert(result.count() === 3)

        //The output field should exist.
        assert(result.schema.fieldIndex("topic") > 0)

      } finally {
        System.setProperty("user.dir", oldDir)
      }
    }
  }
}

case class Tweet (id: String, date: String, text: String)
