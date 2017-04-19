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

package software.uncharted.sparkpipe.ops.xdata.text.transformations

import org.apache.spark.sql.Row
import software.uncharted.sparkpipe.spark.SparkFunSpec
import software.uncharted.sparkpipe.ops.xdata.text.analytics.DictionaryConfig

case class Document(id: Int, word: String)

class TransformationsTests extends SparkFunSpec {

  private val data = Seq(
    Document(1, "Who in the world am I? Ah, that's the great puzzle"),
    Document(2, "Sometimes I believe in as many as six impossible things before breakfast. Breakfast is important not to skip"),
    Document(3, "There are many music festivals this week")
  )
  private val idCol = "id"
  private val wordCol = "word"
  private val goWordsPath = "/go_words.txt"
  private val stopWordsPath = "/stopwords/stopwords_all_en_clean.txt"

  describe("#wordBags") {

    it("should convert dataframeToWordBags") {
      val wordBagConfig = DictionaryConfig(
        false,
        Some(goWordsPath),
        Some(stopWordsPath),
        None, None, None)

      val inputData = sc.parallelize(data)
      val dfData = sparkSession.createDataFrame(inputData)

      val result = dataframeToWordBags(wordBagConfig, idCol, wordCol)(dfData).collect()

      assertResult(Map("puzzle" -> 1))(result(0)._2)
      assertResult(Map("impossible" -> 1, "breakfast" -> 2))(result(1)._2)
      assertResult(Map("music" -> 1, "festivals" -> 1))(result(2)._2)
    }

    it("should convert textToWordBags") {
      val rddData = sc.parallelize( Seq(
        Document(1, "fish taco tuesday"),
        Document(2, "cat saturday")
      ))

      val dfData = sparkSession.createDataFrame(rddData)

      val config = DictionaryConfig(
        false,
        Some(goWordsPath),
        Some(stopWordsPath),
        None, None, None)

      val result = textToWordBags[Row, (Int, Map[String, Int])](
        config,
        _.getString(1),
        (row, wordBag) => (row.getInt(0), wordBag)
      )(dfData.select(idCol, wordCol).rdd).collect()


      assert(result(0) === (1,Map("fish"->1, "taco"->1, "tuesday"->1)))
      assert(result(1) === (2, Map("cat"->1, "saturday"->1)))
    }

    it("should convert rddToWordBags") {
      val wordBagConfig = DictionaryConfig(
        true,
        Some(goWordsPath),
        Some(stopWordsPath),
        None, None, None)

      val rddData = sc.parallelize(data)

      val result = rddToWordBags[Int, Document](wordBagConfig, (input => input.id), (input => input.word))(rddData).collect()

      assertResult(Map("puzzle" -> 1))(result(0)._2)
      assertResult(Map("impossible" -> 1, "breakfast" -> 1))(result(1)._2)
      assertResult(Map("music" -> 1, "festivals" -> 1))(result(2)._2)
    }

    it("should convert wordBagToWordVector") {
      val rddData = sc.parallelize(Seq(
        (1, Map("puzzle" -> 1)),
        ("2", Map("impossible" -> 1, "possible" -> 1)),
        (3, Map("breakfast" -> 1, "lunch" -> 3, "dinner" -> 2))
      ))

      val arrayDictionary = Array(
        ("cats", 1),
        ("koalas", 2),
        ("puzzle", 5),
        ("impossible", 2)
      )

      val result = wordBagToWordVector(rddData, arrayDictionary).collect()

      assert(result(0) === (1, Map(2 -> 1)))
      assert(result(1) === ("2", Map(3 -> 1)))
      assert(result(2) === (3, Map()))
    }

    it("should getDictionary") {
      val rddData = sc.parallelize( Seq(
        Document(1, "fish taco tuesday"),
        Document(2, "cat saturday"),
        Document(4, "cat saturday"),
        Document(5, "throwback tuesday")
      ))
      val dfData = sparkSession.createDataFrame(rddData)

      val config = DictionaryConfig(
        false,
        None, None,
        Some(1.0), Some(0.5), Some(2))

      val inputWords = textToWordBags[Row, (Int, Map[String, Int])](
        config,
        _.getString(1),
        (row, wordBag) => (row.getInt(0), wordBag)
      )(dfData.select(idCol, wordCol).rdd)

      val dictionary = getDictionary[(Int, Map[String, Int])](config, _._2)(inputWords)

      assertResult(("saturday",2))(dictionary(0))
      assertResult(("tuesday",2))(dictionary(1))
    }

    it("should getDictionaries") {
      val rddData = sc.parallelize( Seq(
        Document(1, "fish taco tuesday"),
        Document(23, "taco"),
        Document(2, "cat saturday"),
        Document(4, "sunny sunday")
      ))
      val dfData = sparkSession.createDataFrame(rddData)

      val config = DictionaryConfig(
        false,
        None, None,
        Some(1.0), Some(0.5), Some(4))

      val inputWords = textToWordBags[Row, (Int, Map[String, Int])](
        config,
        _.getString(1),
        (row, wordBag) => (row.getInt(0), wordBag)
      )(dfData.select(idCol, wordCol).rdd).map(row => Array(row))

      val dictionary = getDictionaries[Array[(Int, Map[String, Int])]](config,  _(0))(inputWords)

      dictionary.map(println)

      assert(dictionary(0) == ("fish", Map(1-> 1)))
      assert(dictionary(1) == ("saturday", Map(2-> 1)))
      assert(dictionary(2) == ("taco", Map(23->1,  1-> 1)))
      assert(dictionary(3) == ("tuesday", Map( 1->1)))
    }

    it("should getDictionaries with no maximum dictionary size or documentCount") {
      val rddData = sc.parallelize( Seq(
        Document(1, "cat saturday"),
        Document(2, "sunny sunday")
      ))
      val dfData = sparkSession.createDataFrame(rddData)
      val config = DictionaryConfig(false, None, None, None, None, None)

      val inputWords = textToWordBags[Row, (Int, Map[String, Int])](
        config,
        _.getString(1),
        (row, wordBag) => (row.getInt(0), wordBag)
      )(dfData.select(idCol, wordCol).rdd).map(row => Array(row))

      val dictionary = getDictionaries[Array[(Int, Map[String, Int])]](config,  _(0))(inputWords)

      assert(dictionary(0) == ("cat", Map(1-> 1)))
      assert(dictionary(1) == ("saturday", Map(1-> 1)))
      assert(dictionary(2) == ("sunday", Map(2-> 1)))
      assert(dictionary(3) == ("sunny", Map(2-> 1)))

    }
  }
}