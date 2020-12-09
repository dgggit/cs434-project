/*
 * MIT License
 *
 * Copyright (c) 2020 Jinho Ko
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

package dpsort

import scala.collection.mutable.ArrayBuffer

package object core {

  /* Definition on input data */
  type RecordLines = Array[Array[Byte]]
  type MutableRecordLines = ArrayBuffer[Array[Byte]]

  val LINE_SIZE_BYTES = 100
  val KEY_OFFSET_BYTES = 10

  val MIN_KEY = (0 until 10).map(_ => ' '.toByte).toArray
  val MAX_KEY = (0 until 10).map(_ => '~'.toByte).toArray

  type PartFunc = Array[(Array[Byte], (String, Int))]
  type MutablePartFunc = ArrayBuffer[(Array[Byte], (String, Int))]

}