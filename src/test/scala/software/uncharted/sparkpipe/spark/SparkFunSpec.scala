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
package software.uncharted.sparkpipe.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{FunSpec, Outcome}

/**
 * Makes a spark context available to test subclasses.  The context is created before
 * a test case is run, and destroyed after it completes.
 */
abstract class SparkFunSpec extends FunSpec {
  protected var sc: SparkContext = _
  protected var sparkSession: SparkSession = _

  def before(): Unit = {
    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")

    val conf = new SparkConf()
      .setMaster("local")
      .setAppName("test")

    sparkSession = SparkSession.builder.config(conf).getOrCreate()
    sc = sparkSession.sparkContext
  }

  def after(): Unit = {
    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")
    sc.stop()
  }

  // Use an override here instead of BeforeAndAfter trait to allow for lock release
  // on exceptions.
  override protected def withFixture(test: NoArgTest): Outcome = {
    // make sure spark lock is always released after test is run
    // Force Spark test cases to be run single threaded.
    before()
    val res = super.withFixture(test)
    after()
    res
  }
}
