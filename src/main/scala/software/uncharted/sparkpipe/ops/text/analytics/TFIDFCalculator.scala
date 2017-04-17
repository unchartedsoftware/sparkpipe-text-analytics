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
package software.uncharted.sparkpipe.ops.xdata.text.analytics

/*
 * Possible Additions
 *   vocabulary: Array[String]   use only words in this to compute TF/IDF
 *   stopword: Array[String]     ignore words in this
 *   max_df: Int                 ignore words above this threshold (automatically detect and filter stopwords)
 *   min_df: Int                 ignore words below this threshold
 *   max_features: Int           build a vocabulary using only the top max_features words
 *   lowercase: Boolean          optionally lowercase all words
 *
 *
 * Normalization:
 *   Sometimes a normalization factor is used to correct discrepencies in document lengths.
 *   Document vectors are normalized so that documents are retrieved independent of their lengths.
 *   If this is not done and lengths vary considerably, very short documents may not be recognized as being relevant.
 *   Cosine Normalization:
 *     1 / math.sqrt( math.pow(w1, 2) + math.pow(w2, 2) +...+ math.pow(wn, 2) )
 *
 *     where w is the weight of term i
 *
 *
 * TFIDF calculation
 *   should there be a TFIDFCalculator object to compute:  tf * idf
 *
 *
 * IDF
 *   maybe refactor to use two flags:
 *   smooth_idf: Boolean     whether or not to add +1 smoothing
 *   log_idf: Boolean        whether or not to apply log(IDF)
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
  * Binary term frequency - 1 if term is present in the current document, 0 if it is absent
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










