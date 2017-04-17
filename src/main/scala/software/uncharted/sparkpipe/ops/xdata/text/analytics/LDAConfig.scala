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


package software.uncharted.sparkpipe.ops.xdata.text.analytics

/**
  * A configuration describing LDA-specific parameters to using when performing LDA
  *
  * @param numTopics The number of topics the LDA analysis is instructed to find
  * @param wordsPerTopic The number of words the LDA analysis should use per topic
  * @param topicsPerDocument The number of topics the LDA analysis should assume per document
  * @param maxIterations An optional number of maximum iterations to use when performing LDA analysis
  * @param chkptInterval The number of iterations to perform between checkpoints, if checkpoints can be taken.
  * @param topicSeparator A separator to use between topics when outputting LDA results
  * @param wordSeparator A separator to use between word/score pairs when outputting LDA results
  * @param scoreSeparator A separator to use between a word and its score when outputting LDA results
  */
case class LDAConfig(numTopics: Int,
                     wordsPerTopic: Int,
                     topicsPerDocument: Int,
                     chkptInterval: Option[Int],
                     maxIterations: Option[Int],
                     topicSeparator: String,
                     wordSeparator: String,
                     scoreSeparator: String)
