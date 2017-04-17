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
