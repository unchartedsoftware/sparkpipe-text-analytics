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

import org.apache.spark.mllib.linalg.{DenseMatrix, DenseVector, Matrix, SparseMatrix, SparseVector, Vector}


/**
  * Some general utilities for use with MLLib matrices
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
