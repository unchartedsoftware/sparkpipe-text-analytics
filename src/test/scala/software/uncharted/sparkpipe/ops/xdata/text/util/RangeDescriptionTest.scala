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

package software.uncharted.salt.xdata.util

import org.scalatest.FunSpec
import software.uncharted.sparkpipe.ops.xdata.text.util.RangeDescription

// scalastyle:off magic.number
class RangeDescriptionTest extends FunSpec {
  describe("RangeDescriptionTest") {
    describe("#constructor") {
      it("should throw an IllegalArgumentException if min isn't less than max") {
        val min = 2d
        val max = 1d
        val caughtMsg = intercept[IllegalArgumentException] {
          RangeDescription(min, max, 0, .1d)
        }

        assert(caughtMsg.getMessage() == s"requirement failed: min ($min) must be less than max ($max)")
      }

      it("should throw an IllegalArgumentException if count isn't greater than 1") {
        val count = -1
        val caughtMsg =  intercept[IllegalArgumentException] {
          RangeDescription(0d, 1d, count, .1d)
        }

        assert(caughtMsg.getMessage() == s"requirement failed: count ($count) must be greater than 1")
      }

      it("should throw an IllegalArgumentException if step isn't positive") {
        val step = -0.1d
        val caughtMsg = intercept[IllegalArgumentException] {
          RangeDescription(0d, 1d, 1, step)
        }

        assert(caughtMsg.getMessage() == s"requirement failed: step ($step) must be greater than 0")
      }
    }

    describe("#fromMin") {
      it("should create a range with the correct max given min, count and step") {
        assertResult(RangeDescription(10.0, 20.0, 10, 1.0))(RangeDescription.fromMin(10.0, 1.0, 10))
      }
    }

    describe("#fromMax") {
      it("should create a range with the correct min given max, count and step") {
        assertResult(RangeDescription(10.0, 20.0, 10, 1.0))(RangeDescription.fromMax(20.0, 1.0, 10))
      }
    }

    describe("#fromCount") {
      it("should create a range with the correct count given min, max and step") {
        assertResult(RangeDescription(10.0, 20.0, 10, 1.0))(RangeDescription.fromCount(10.0, 20.0, 10))
      }
    }

    describe("#fromStep") {
      it("should create a range with the correct count given min, max, and step") {
        assertResult(RangeDescription(10.0, 20.0, 10, 1.0))(RangeDescription.fromStep(10.0, 20.0, 1.0))
      }
    }
  }
}
