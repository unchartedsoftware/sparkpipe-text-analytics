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

package software.uncharted.sparkpipe.ops.text.util

import org.apache.spark.mllib.linalg.{DenseMatrix, DenseVector, Matrix, SparseMatrix, SparseVector, Vector}


/**
  * Some general utilities for use with MLLib matrices
  *
  * @author nkronenfeld
  */
object MatrixUtilities {
  /**
    * Pull a single column out of a matrix
    * @param matrix The matrix from which to extract the column
    * @param column The column index to extract
    * @return The extracted column
    */
  def column (matrix: Matrix, column: Int): Vector = {
    matrix match {
      case sm: SparseMatrix => sparseColumn(sm, column)
      case dm: DenseMatrix  => denseColumn(dm, column)
    }
  }

  private def sparseColumn (matrix: SparseMatrix, column: Int): SparseVector = {
    if (matrix.isTransposed) {
      val columnBounds = matrix.colPtrs.sliding(2).map(bounds => Range(bounds(0), bounds(1))).zipWithIndex

      // Combine row and column indices
      val rcCoords = matrix.rowIndices.zipWithIndex.filter { case (row, index) =>
        column == row
      }.map{case (row, index) =>
        val column = columnBounds.find(_._1.contains(index)).get._2
        (row, column, index)
      }
      val indices = rcCoords.map(_._2).toArray
      val values = rcCoords.map(_._3).map(c => matrix.values(c)).toArray
      new SparseVector(matrix.numRows, indices, values)
    } else {
      val start = matrix.colPtrs(column)
      val end = matrix.colPtrs(column + 1)

      new SparseVector(matrix.numRows, matrix.rowIndices.slice(start, end), matrix.values.slice(start, end))
    }
  }

  private def denseColumn (matrix: DenseMatrix, column: Int): DenseVector = {
    val start = matrix.numRows * (column + 0)
    val end = matrix.numRows * (column + 1)

    new DenseVector(matrix.values.slice(start, end))
  }

  /**
    * Pull a single row out of a matrix
    * @param matrix The matrix from which to extract the row
    * @param row The row index to extract
    * @return The extracted row
    */
  def row (matrix: Matrix, row: Int): Vector = {
    matrix match {
      case sm: SparseMatrix => sparseColumn(sm.transpose, row)
      case dm: DenseMatrix => denseRow(dm, row)
    }
  }

  private def denseRow (matrix: DenseMatrix, row: Int): DenseVector = {
    new DenseVector((0 until matrix.numCols).map(n => matrix.values(row + n*matrix.numRows)).toArray)
  }
}
