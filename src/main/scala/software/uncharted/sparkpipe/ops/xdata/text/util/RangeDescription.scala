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

import spire.implicits._ // scalastyle:ignore
import spire.math.Numeric

object RangeDescription {
  def fromMin[T : Numeric](min: T, step: T, count: Int): RangeDescription[T] = RangeDescription(min, min + step*count, count, step)
  def fromMax[T : Numeric](max: T, step: T, count: Int): RangeDescription[T] = RangeDescription(max - (step*count), max, count, step)
  def fromCount[T : Numeric](min: T, max: T, count: Int): RangeDescription[T] = RangeDescription(min, max, count, (max - min) / count)
  def fromStep[T : Numeric](min: T, max: T, step: T): RangeDescription[T] = RangeDescription(min, max, ((max - min) / step).toInt(), step)
}

case class RangeDescription[T: Numeric](min: T, max: T, count: Int, step: T) {
  require(min < max, s"min ($min) must be less than max ($max)")
  require(count >= 1, s"count ($count) must be greater than 1")
  require(step > 0, s"step ($step) must be greater than 0")
}
