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
package software.uncharted.sparkpipe.ops.text.analytics

import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.mllib.feature.{HashingTF => TestTF}
import software.uncharted.sparkpipe.ops.text.analytics
import software.uncharted.sparkpipe.spark.SparkFunSpec

class TextTest extends SparkFunSpec {
  describe("Pipeline Text Analytics Test") {
    it("should output terms & their TF/IDF values") {
      val rddData = sc.parallelize(Seq(
        TFIDFData(1, "this is a test".split(" ")),
        TFIDFData(2, "more testing done".split(" ")),
        TFIDFData(3, "test it again sam".split(" ")),
        TFIDFData(4, "i wish i were testing so that i could do it".split(" ")),
        TFIDFData(5, "this is the final test i did it".split(" "))
      ))

      val dfData = sparkSession.createDataFrame(rddData)

      val result = analytics.tfidf("text", "tfs", "tfidfs")(dfData)

      assert(result.count() === 5)

      //Verify the relative tfidf values for a few pairs.
      //Extract the term + tfidf values.
      val resultMap = result.select("tfidfs").collect().map(x => x(0).asInstanceOf[SparseVector])

      //Use the hashing TF to get the index in the sparse vectors returned.
      val testTF = new TestTF(resultMap(0).size)

      assert(resultMap(0)(testTF.indexOf("test")) < resultMap(0)(testTF.indexOf("this")))
      assert(resultMap(0)(testTF.indexOf("this")) < resultMap(0)(testTF.indexOf("a")))

      assert(resultMap(1)(testTF.indexOf("testing")) < resultMap(1)(testTF.indexOf("more")))
      assert(resultMap(1)(testTF.indexOf("testing")) < resultMap(1)(testTF.indexOf("done")))

      assert(resultMap(3)(testTF.indexOf("wish")) < resultMap(3)(testTF.indexOf("i")))
      assert(resultMap(3)(testTF.indexOf("it")) < resultMap(3)(testTF.indexOf("wish")))

      assert(resultMap(4)(testTF.indexOf("it")) < resultMap(4)(testTF.indexOf("final")))

      assert(resultMap(4)(testTF.indexOf("i")) < resultMap(3)(testTF.indexOf("i")))
    }
  }
}

case class TFIDFData (id: Integer, text: Seq[String])
