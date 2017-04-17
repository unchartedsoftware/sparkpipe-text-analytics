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
  * Configuration specifying how TF*IDF is to be performed
  *
  * @param tf The algorithm to perform the term-frequency calculation
  * @param idf The algorithm to perform the inverse-document-frequency calculation
  */
case class TFIDFConfig(tf: TFType,
                       idf: IDFType,
                       dictionaryConfig: DictionaryConfig,
                       wordsToKeep: Int)
