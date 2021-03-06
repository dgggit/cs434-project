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

package dpsort.core.utils

import scala.collection.mutable
import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream, Serializable}

import org.apache.logging.log4j.scala.Logging
import com.google.protobuf.ByteString


object SerializationUtils extends Logging {

  def serializeObjectToByteArray( obj: Serializable ): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(obj)
    oos.close()
    stream toByteArray
  }
  def serializeObjectToByteString( obj: Serializable ): ByteString = {
    ByteString copyFrom serializeObjectToByteArray(obj)
  }

  def deserializeByteArrayToObject[T]( byteArr: Array[Byte] ): T = {
    assert( byteArr.size != 0 )
    val stream = new ByteArrayInputStream( byteArr )
    val ois = new ObjectInputStream(stream)
    val outputObj = ois.readObject().asInstanceOf[T]
    ois.close()
    outputObj
  }
  def deserializeByteStringToObject[T]( byteStr: ByteString ): T = {
    assert( byteStr.size != 0 )
    deserializeByteArrayToObject( byteStr.toByteArray )
  }

  def getEmptyByteString: ByteString = serializeObjectToByteString( "NULL" )

}
