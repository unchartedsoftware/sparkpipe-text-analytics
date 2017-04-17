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

package software.uncharted.sparkpipe.ops.xdata.text.twitter.util

import grizzled.slf4j.Logging
import scala.util.Random


/**
  * data structure to keep track of which topic z is assigned to each biterm:
  * n.b. a map like nwz isn't good since there are so many biterms.
  *
  * Things this class does:
  *   - create & initialize sample recorders
  *   - increment counts in sample recorders
  *   - decrement counts in sample recorders
  *   - add new cluster
  *   - remove cluster
  *   - defragment clusters
  *   - draw sample from distribution
  *
  */
object Records extends Serializable {
  def wordCountArray(m: Int) : Array[Double] = Array.fill[Double](m)(0)
  def createRecorders(m: Int, k: Int): (scala.collection.mutable.Map[Int,Int], scala.collection.mutable.Map[Int,Array[Double]]) = {
    // counts of how many words for each value of z
    val nwz = collection.mutable.Map(Iterator.range(0, k).map { z => z -> wordCountArray(m) }.toSeq: _*)
    // counts of how many biterms in each cluster
    val nz = collection.mutable.Map(Iterator.range(0, k).map { z => z -> 0 }.toSeq: _*)
    (nz, nwz)
  }
}



class SampleRecorder (m: Int, kK:Int, weighted: Boolean = false) extends Serializable with Logging {
  if (weighted) {
    info("Using term weights rather than +/- 1. Input TFIDF dictionary")
  }
  // Initialize class variables
  var tfidf_dict: Map[Int,Double] = Map[Int,Double]()             // empty placeholder for tfidf_dict

  def setTfidf(tfidf_map:  Map[Int, Double]) : Unit = {
    tfidf_dict = tfidf_map
  }

  val (nz, nwz) = Records.createRecorders(m, kK)

  def initRecorders(biterms: Array[Biterm] ) : Unit = {
    if (weighted) assert(tfidf_dict.size > 0)       // n.b. have to setTfIdf dict if weighted=true before initializing recorders
    def uniSample(k: Int) : Int = { Random.nextInt(k) }
    var count = 0
    biterms.foreach {b =>
      val z = uniSample(kK)
      count += 1
      b.z = z
      increment(z, b.biterm)
    }
  }


  def getWeight(word: Int, default_value: Double = 0.0001): Double = {
    tfidf_dict.getOrElse(word, default_value)
  }

  def increment(z: Int, biterm: (Int, Int)) : Unit = {
    val (w1, w2) = biterm
    nz(z) += 1
    nwz(z)(w1) = if (weighted) nwz(z)(w1) + getWeight(w1) else nwz(z)(w1) + 1
    nwz(z)(w2) = if (weighted) nwz(z)(w2) + getWeight(w1) else nwz(z)(w2) + 1
  }

  def decrement(biterm: (Int, Int), z: Int, k: Int ) : Unit = {
    val (w1, w2) = biterm // if z > k there will be nothing to decrement since that cluster has been deleted for being empty
     if ((z < k) & (k >0)) {
      // don't decrement below 0
      if (nz(z) > 0) nz(z) -= 1
      if (nwz(z)(w1) > 0) nwz(z)(w1) = if (weighted) nwz(z)(w1) - getWeight(w1) else  nwz(z)(w1) - 1
      if (nwz(z)(w2) > 0) nwz(z)(w2) = if (weighted) nwz(z)(w2) - getWeight(w1) else  nwz(z)(w2) - 1
    }
  }


  // ----------    Accessor methods: Get/Set Sample Recorders   ---------- //
  def getNzMap() : scala.collection.mutable.Map[Int,Int] = nz
  def getNwzMap() : scala.collection.mutable.Map[Int,Array[Double]] = nwz
  def getNz(z: Int) : Int = nz(z)
  def getNwz(z: Int, w: Int) : Double = nwz(z)(w)
  def size() : Int = nz.size

  // -----------   Mutate/Modify Sample Recorders   ---------- //
  def addCluster() : Int = {
    var k = nz.size
    nz += k -> 0
    nwz += k -> Records.wordCountArray(m)
    k += 1
    k
  }

  private def mapZ() = {
    val keys = nz.keys.toArray.sorted
    var old2newZ = scala.collection.mutable.Map[Int, Any]()
    var next = 0
    for (k <- keys) {
      if (nz(k) > 0) {
        old2newZ += k -> next
        next += 1
      }
    }
    old2newZ
  }

  private def removeEmptyClusters() = {
    var k = nz.size
    val zeros = nz.keys.toArray.filter(z => nz(z) == 0).sorted
    // if (k > 1) { }
    for (z <- zeros) {
      nz -= z
      nwz -= z
      k -= 1
    }

    k
  }

  // get rid of empty clusters (empty tables can be removed from CRP)
  def defrag() : Int = {
    val oldk = nz.size
    val old2newZ = mapZ()
    val k = removeEmptyClusters
    val tomove = old2newZ.filter{case (k, v) => k != v }
    tomove.toSeq.sortWith(_._1 < _._1).foreach{ case(k:Int, v:Int) =>
      nz += v -> nz(k)
      nz -= k
      nwz += v -> nwz(k)
      nwz -= k
    }
    k
  }
}
