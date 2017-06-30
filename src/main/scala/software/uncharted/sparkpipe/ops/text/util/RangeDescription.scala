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

import spire.implicits._ // scalastyle:ignore
import spire.math.Numeric

object RangeDescription {
  /**
    * Creates a description of a numeric range with an inferred max value.
    *
    * @param min min value of the range
    * @param step range increment
    * @param count number of increments
    * @tparam T numeric type associated with the range
    * @return the range description
    */
  def fromMin[T : Numeric](min: T, step: T, count: Int): RangeDescription[T] = RangeDescription(min, min + step*count, count, step)

  /**
    * Creates a description of a numeric range with an inferred min value.
    *
    * @param max max value of the range
    * @param step range increment
    * @param count number of increments
    * @tparam T numeric type associated with the range
    * @return the range description
    */
  def fromMax[T : Numeric](max: T, step: T, count: Int): RangeDescription[T] = RangeDescription(max - (step*count), max, count, step)

  /**
    * Creates a description of a numeric range with an inferred step value.
    *
    * @param min min value of the range
    * @param max max value of the range
    * @param count number of increments
    * @tparam T numeric type associated with the range
    * @return the range description
    */
  def fromCount[T : Numeric](min: T, max: T, count: Int): RangeDescription[T] = RangeDescription(min, max, count, (max - min) / count)

  /**
    * Creates a description of a numeric range with an inferred count value.
    *
    * @param min min value of the range
    * @param max max value of the range
    * @param step range increment
    * @tparam T numeric type associated with the range
    * @return the range description
    */
  def fromStep[T : Numeric](min: T, max: T, step: T): RangeDescription[T] = RangeDescription(min, max, ((max - min) / step).toInt(), step)
}

/**
  * A description of a numeric range.  Does not implement the range itself.
  *
  * @param min min value of the range
  * @param max max value of the range
  * @param count number of increments
  * @param step range increment  * @tparam T
  * @tparam T numeric type associated with the range
  * @return the range description
  */
case class RangeDescription[T: Numeric](min: T, max: T, count: Int, step: T) {
  require(min < max, s"min ($min) must be less than max ($max)")
  require(count >= 1, s"count ($count) must be greater than 1")
  require(step > 0, s"step ($step) must be greater than 0")
}
