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
