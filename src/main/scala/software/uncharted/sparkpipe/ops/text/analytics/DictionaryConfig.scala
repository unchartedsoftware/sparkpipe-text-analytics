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


package software.uncharted.sparkpipe.ops.text.analytics

/**
  * Configuration delineating how to construct a dictionary for use by a text analytic
  *
  * @param caseSensitive Whether or not dictionary determination should be case-sensitive
  * @param dictionary An optional file containing the complete dictionary to use
  * @param stopwords An optional file containing a list of words not to use in the dictionary
  * @param maxDF An optional maximum document frequency (as a proportion of the total number of documents) above
  *              which terms will not be used in the dictionary
  * @param minDF An optional minimum document frequency (as a proportion of the total number of documents) below
  *              which terms will not be used in the dictionary
  * @param maxFeatures An optional maximum dictionary size.
  */
case class DictionaryConfig(caseSensitive: Boolean,
                            dictionary: Option[String],
                            stopwords: Option[String],
                            maxDF: Option[Double],
                            minDF: Option[Double],
                            maxFeatures: Option[Int]) {
  def needDocCount: Option[Boolean] = if (minDF.isDefined || maxDF.isDefined) {
    Some(true)
  } else {
    None
  }
}
