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
package software.uncharted.sparkpipe.ops.xdata.text.util

import org.apache.spark.mllib.linalg.{DenseMatrix, DenseVector, SparseMatrix, Vector}
import org.scalatest.FunSpec

class MatrixUtilityTests extends FunSpec {
  private def vector (entries: Double*): Vector = new DenseVector(entries.toArray)
  // Matrix is
  // |  0  3  0  0 |
  // |  2  4  0  0 |
  // |  0  0  0  5 |
  val sm = new SparseMatrix(3, 4, Array(0, 1, 3, 3, 4), Array(1, 0, 1, 2), Array(2.0, 3.0, 4.0, 5.0), false)
  val dm = new DenseMatrix(3, 4, Array(0.0, 2.0, 0.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0))

  describe("Column operations") {
    import MatrixUtilities._

    it("should pull out a column of an untransposed sparse matrix correctly") {
      assert(vector(0.0, 2.0, 0.0) === column(sm, 0))
      assert(vector(3.0, 4.0, 0.0) === column(sm, 1))
      assert(vector(0.0, 0.0, 0.0) === column(sm, 2))
      assert(vector(0.0, 0.0, 5.0) === column(sm, 3))
    }

    it("should pull out a column of a transposed sparse matrix correctly") {
      assert(vector(0.0, 3.0, 0.0, 0.0) === column(sm.transpose, 0))
      assert(vector(2.0, 4.0, 0.0, 0.0) === column(sm.transpose, 1))
      assert(vector(0.0, 0.0, 0.0, 5.0) === column(sm.transpose, 2))
    }

    it("should pull out a column of a dense matrix correctly") {
      assert(vector(0.0, 2.0, 0.0) === column(dm, 0))
      assert(vector(3.0, 4.0, 0.0) === column(dm, 1))
      assert(vector(0.0, 0.0, 0.0) === column(dm, 2))
      assert(vector(0.0, 0.0, 5.0) === column(dm, 3))
    }
  }

  describe("Row operations") {
    import MatrixUtilities._

    it("should pull out a row of a transposed sparse matrix correctly") {
      assert(vector(0.0, 2.0, 0.0) === row(sm.transpose, 0))
      assert(vector(3.0, 4.0, 0.0) === row(sm.transpose, 1))
      assert(vector(0.0, 0.0, 0.0) === row(sm.transpose, 2))
      assert(vector(0.0, 0.0, 5.0) === row(sm.transpose, 3))
    }

    it("should pull out a row of an untransposed sparse matrix correctly") {
      assert(vector(0.0, 3.0, 0.0, 0.0) === row(sm, 0))
      assert(vector(2.0, 4.0, 0.0, 0.0) === row(sm, 1))
      assert(vector(0.0, 0.0, 0.0, 5.0) === row(sm, 2))
    }

    it("should pull out a row of a dense matrix correctly") {
      assert(vector(0.0, 3.0, 0.0, 0.0) === row(dm, 0))
      assert(vector(2.0, 4.0, 0.0, 0.0) === row(dm, 1))
      assert(vector(0.0, 0.0, 0.0, 5.0) === row(dm, 2))
    }
  }
}
