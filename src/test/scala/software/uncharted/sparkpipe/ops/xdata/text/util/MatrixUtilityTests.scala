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
