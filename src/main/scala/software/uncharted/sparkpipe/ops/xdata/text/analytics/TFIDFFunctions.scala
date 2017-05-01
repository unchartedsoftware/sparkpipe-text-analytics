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
  * Provides functions for evaluating term frequency (TF) and invers document frequency (IDF) in
  * a number of standard ways.
  *
  * @author nkronenfeld
  */

sealed trait TFType extends Serializable {
  /** Calculate the term frequency of a given term in a given document.
    * Note that neither the term in question or the document are actually given, just statistically described.
    *
    * @param rawF The number of times the given term appears in the current document
    * @param nTerms The number of terms in the current document
    * @param maxRawF The maximum number of times any given term appears in the current document
    * @return The TF score for this term in this document
    */
  def termFrequency (rawF: Int, nTerms: Int, maxRawF: Int): Double
}

/**
  * Binary term frequency - 1 if term is present in the current document, 0 if it is absent.
  */
object BinaryTF extends TFType {
  override def termFrequency(rawF: Int, nTerms: Int, maxRawF: Int): Double = {
    if (rawF > 0) {
      1
    } else {
      0
    }
  }
}

/**
  * Raw term frequency - just the count of the number of occurances of theterm in the current document
  */
object RawTF extends TFType {
  override def termFrequency(rawF: Int, nTerms: Int, maxRawF: Int): Double = {
    rawF.toDouble
  }
}

/**
  * Term frequency, normalized by the size of the document
  */
object DocumentNormalizedTF extends TFType {
  override def termFrequency(rawF: Int, nTerms: Int, maxRawF: Int): Double = {
    if (0 == nTerms) {
      0
    } else {
      rawF.toDouble / nTerms.toDouble
    }
  }
}

/**
  * Sublinear term frequency scaling. This is a variant of TF that goes beyond counting occurences shrinking the range
  * eoung so as to put everything into a comprehensible range. It captures the intuition that, for example, ten
  * occurrences of a term in a document is unlikely to carry ten times the significance
  * of a since occurence.
  */
object SublinearTF extends TFType {
  override def termFrequency(rawF: Int, nTerms: Int, maxRawF: Int): Double = {
    if (rawF > 0) {
      1.0 + math.log(rawF)
    } else {
      // raw frequency of 0 should remain 0
      0
    }
  }
}

/**
  * Term frequency, normalized by the maximum term frequency in the current document (to prevent a preference for
  * longer documents)
  *
  * @param k The minimum allowed TF score.  K must be in the range [0, 1), and all TF scores will fall in the range
  *          [k, 1]
  */
case class TermNormalizedTF (k: Double) extends TFType {
  assert(0.0 <= k)
  assert(k < 1.0)
  override def termFrequency(rawF: Int, nTerms: Int, maxRawF: Int): Double = {
    k + (1.0 - k) * rawF / maxRawF
  }
}



sealed trait IDFType extends Serializable {
  /** Calculate the inverse document frequency of a given term with respect to a given document.
    * Note that neither the term in question or the document are actually given, just statistically described.
    *
    * @param N The total number of documents
    * @param n_t The number of documents containing the current term
    * @return The inverse document frequency of the given term.
    */
  def inverseDocumentFrequency (N: Long, n_t: Int): Double
}

/**
  * Unary inverse document frequency - always 1
  */
object UnaryIDF extends IDFType {
  override def inverseDocumentFrequency(N: Long, n_t: Int): Double = {
    1.0
  }
}

/**
  * Linear inverse document frequency - simply 1/document frequency
  */
object LinearIDF extends IDFType {
  override def inverseDocumentFrequency(N: Long, n_t: Int): Double = {
    N.toDouble / n_t.toDouble
  }
}

/**
  * Log inverse document frequency - keep ranges smaller, more understandable through use of logs
  */
object LogIDF extends IDFType {
  override def inverseDocumentFrequency(N: Long, n_t: Int): Double = {
    math.log(N.toDouble / n_t.toDouble)
  }
}

/**
  * Smooth log inverse document frequency - further shrinking of range from unsmoothed log by forcing frequencies
  * above one
  */
object SmoothLogIDF extends IDFType {
  override def inverseDocumentFrequency(N: Long, n_t: Int): Double = {
    math.log(1.0 + N.toDouble / n_t.toDouble)
  }
}


/**
  * This variant of IDF assigns weights ranging from -infinity for a term that appears in every document
  * to log(n-1) for a term that appears in only one document. Thus, unlike log IDF  it assigns negative
  * weights to terms that appear in more than half of the documents.
  */
object ProbabilisticIDF extends IDFType {
  override def inverseDocumentFrequency(N: Long, n_t: Int): Double = {
    math.log((N.toDouble - n_t.toDouble) / n_t.toDouble)
  }
}










