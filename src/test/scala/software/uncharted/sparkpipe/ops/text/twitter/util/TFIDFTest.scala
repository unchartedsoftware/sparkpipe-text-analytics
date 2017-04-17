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
package software.uncharted.sparkpipe.ops.text.twitter.util

import org.apache.spark.sql.Row
import org.apache.spark.sql.types._
import software.uncharted.sparkpipe.spark.SparkFunSpec


class TFIDFTest extends SparkFunSpec {
  private val testSequence = Seq(
    Row("2017-0101", "cat", 1.0),
    Row("2017-0105", "books", 1.5),
    Row("2015-0101", "plants", 1.6),
    Row("2016-0101", "trees", 3.9))

  describe("#test various TFIDF functions") {

    it("should test loadTFIDF") {
      val data = sc.parallelize(testSequence)
      val schema = StructType(Seq(StructField("a", StringType), StructField("b", StringType), StructField("c", DoubleType)))
      val converted = sparkSession.createDataFrame(data, schema)
      val sampleTFIDF = TFIDF.loadTFIDF(converted)
      val expected = Array(("2017-0101","cat", 1.0),("2017-0105", "books", 1.5),("2015-0101", "plants", 1.6), ("2016-0101", "trees", 3.9))

      assertResult(expected)(sampleTFIDF)
    }

    it ("should should filter a TFIDF based on a date range") {
      val data = sc.parallelize(testSequence)
      val schema = StructType(Seq(StructField("a", StringType), StructField("b", StringType), StructField("c", DoubleType)))
      val df = sparkSession.createDataFrame(data, schema)
      val sampleTFIDF = TFIDF.loadTFIDF(df)
      val dates = Array("2017-0101", "2017-0105")
      val result = TFIDF.filterDateRange(sampleTFIDF, dates)
      val expected = Array(("2017-0101","cat",1.0), ("2017-0105","books",1.5))

      assertResult(expected)(result)
    }

    it ("should filter a TFIDF based on a given word dictionary") {
      val data = sc.parallelize(testSequence)
      val schema = StructType(Seq(StructField("a", StringType), StructField("b", StringType), StructField("c", DoubleType)))
      val df = sparkSession.createDataFrame(data, schema)
      val sampleTFIDF = TFIDF.loadTFIDF(df)
      val word_dict = Map("cat"->1, "trees"->2)
      val result = TFIDF.filterWordDict(sampleTFIDF, word_dict)
      val expected = Array(("2017-0101","cat",1.0), ("2016-0101","trees",3.9))

      assertResult(expected)(result)
    }

    it ("should convert a tfidf array to a map") {
      val data = sc.parallelize(testSequence)
      val schema = StructType(Seq(StructField("a", StringType), StructField("b", StringType), StructField("c", DoubleType)))
      val df = sparkSession.createDataFrame(data, schema)
      val sampleTFIDF = TFIDF.loadTFIDF(df)
      val word_dict = Map("cat"->1, "trees"->2, "books"->5, "plants"->3)
      val result = TFIDF.tfidfToMap(sampleTFIDF, word_dict)
      val expected = Map(1->1.0, 5->1.5, 3->1.6, 2->3.9)

      assertResult(expected)(result)
    }
  }

}
