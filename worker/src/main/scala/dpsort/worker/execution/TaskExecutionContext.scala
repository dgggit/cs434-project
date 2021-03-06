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

package dpsort.worker.execution

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import com.google.protobuf.ByteString
import org.apache.logging.log4j.scala.Logging
import dpsort.core.execution._
import dpsort.core.execution.TaskType
import dpsort.core.utils.FileUtils._
import dpsort.core.utils.{FileUtils, SortUtils}
import dpsort.core.utils.SerializationUtils.serializeObjectToByteString
import dpsort.core.{KEY_OFFSET_BYTES, LINE_SIZE_BYTES, MutableRecordLines, PartFunc, RecordLines}
import dpsort.worker.utils.PartitionUtils._
import dpsort.worker.WorkerConf._
import dpsort.worker.ShuffleManager
import dpsort.worker.WorkerParams.OUTPUT_DIR_STR


object ExecCtxtFetcher {
  def getContext( task: BaseTask ): TaskExecutionContext = {
    val tType = task.getTaskType
    tType match {
      case TaskType.EMPTYTASK => EmptyContext
      case TaskType.GENBLOCKTASK => GenBlockContext
      case TaskType.TERMINATETASK => TerminateContext
      case TaskType.LOCALSORTTASK => LocalSortContext
      case TaskType.SAMPLEKEYTASK => SampleKeyContext
      case TaskType.PARTITIONANDSHUFFLETASK => PartitionAndShuffleContext
      case TaskType.MERGETASK => MergeContext
    }
  }
}


trait TaskExecutionContext {
  def run( _task: BaseTask ): Either[Unit, ByteString]
}

object EmptyContext extends TaskExecutionContext {

  def run( _task: BaseTask ) = {
    val task = _task.asInstanceOf[EmptyTask]
    val rndTime = new scala.util.Random(task.getId).nextInt(10)
    println(s"this is emptytask : wait for ${rndTime}(s) and finish");
    Thread.sleep( rndTime * 1000 )

    Left( Unit )
  }

}

object GenBlockContext extends TaskExecutionContext with Logging {

  def run( _task: BaseTask ) = {
    val task = _task.asInstanceOf[GenBlockTask]
    try {
      val filepath = task.inputPartition.head
      for( (outPartName,pIdx) <- task.outputPartition.zipWithIndex ){
        val stIdx = task.offsets(pIdx)._1 - 1
        val copyLen = task.offsets(pIdx)._2 - task.offsets(pIdx)._1 + 1

        val partLinesArr: RecordLines = fetchLinesFromFile( filepath, stIdx, copyLen, LINE_SIZE_BYTES )
        writeLinesToFile( partLinesArr, getPartitionPath(outPartName) )
      }
      Left( Unit )
    } catch {
      case e: Throwable => {
        logger.error("failed to write partition")
        throw e
      }
    }
  }

}

object LocalSortContext extends TaskExecutionContext with Logging {

  def run(_task: BaseTask) = {
    val task = _task.asInstanceOf[LocalSortTask]
    try {
      val filepath = getPartitionPath( task.inputPartition.head )
      val outPartName = task.outputPartition.head
      val partLines: RecordLines = fetchLinesFromFile( filepath, LINE_SIZE_BYTES )
      SortUtils.sortLines(partLines)
      writeLinesToFile( partLines, getPartitionPath(outPartName) )
      deleteFile( filepath )
      Left( Unit )
    } catch {
      case e: Throwable => {
        logger.error("failed to write partition")
        throw e
      }
    }
  }

}

object SampleKeyContext extends TaskExecutionContext with Logging {

  override def run(_task: BaseTask) = {
    val task = _task.asInstanceOf[SampleKeyTask]
    try {
      val filepath = getPartitionPath( task.inputPartition.head )
      val partLines: RecordLines = fetchLinesFromFile( filepath, LINE_SIZE_BYTES )
      val sampledKeys = SortUtils.sampleKeys( partLines, task.sampleRatio, KEY_OFFSET_BYTES )
      val returnObj = serializeObjectToByteString( sampledKeys )
      Right( returnObj )
    } catch {
      case e: Throwable => {
        logger.error("failed to sample partition")
        throw e
      }
    }
  }

}

object PartitionAndShuffleContext extends TaskExecutionContext with Logging {

  def run( _task: BaseTask ) = {
    val task = _task.asInstanceOf[PartitionAndShuffleTask]

    val filepath = getPartitionPath( task.inputPartition.head )
    val partFunc: PartFunc = task.partitionFunc
    val partitions = Array.fill[MutableRecordLines]( partFunc.size )( new ArrayBuffer[Array[Byte]]() )

    val nLines: Int = getNumLinesInFile( filepath )
    SortUtils.splitPartitions( filepath, partFunc, partitions, nLines,  LINE_SIZE_BYTES )

    logger.debug(s"partitioning done : ")
    partFunc.zip( partitions ).foreach( pfpt => { logger.debug(s"(${pfpt._1._2._1}, ${pfpt._1._2._2}) : ${pfpt._2.size} lines") } )
    val partToStoreIdx = partFunc.zipWithIndex
      .filter( pi => ( pi._1._2._1 equals get("dpsort.worker.ip") )
                    && pi._1._2._2 == get("dpsort.worker.shufflePort").toInt )
      .head._2
    // write locally first
    val linesToStore: Array[Array[Byte]] = partitions(partToStoreIdx).toArray
    writeLinesToFile( linesToStore, getPartitionPath( task.outputPartition(partToStoreIdx) ) )
    // shuffle out the rest
    ShuffleManager.shuffleOut( task, partFunc, partitions, partToStoreIdx)
    deleteFile( filepath )
    Left( Unit )
  }

}

object MergeContext extends TaskExecutionContext {

  def run( _task: BaseTask) = {
    val task = _task.asInstanceOf[MergeTask]

    val input1FilePath = getPartitionPath( task.inputPartition(0) )
    val input2FilePath = getPartitionPath( task.inputPartition(1) )
    val outputFilePath = getPartitionPath( task.outputPartition(0) )
    SortUtils.mergePartitions( input1FilePath, input2FilePath, outputFilePath, LINE_SIZE_BYTES)
    deleteFile( input1FilePath )
    deleteFile( input2FilePath )
    Left( Unit )
  }

}


object TerminateContext extends TaskExecutionContext with Logging {
  def run( _task: BaseTask ) = {
    val task = _task.asInstanceOf[TerminateTask]

    val resultFilePath = getPartitionPath( task.inputPartition(0) )
    if( ! (task.outputPartition(0) equals "") ) {
      val resultOutputPath = getAbsPath( OUTPUT_DIR_STR )+"/"+task.outputPartition(0)
      logger.info(s"writing output file to : ${resultOutputPath}")
      FileUtils.moveFile( resultFilePath, resultOutputPath )
    } else {
      FileUtils.deleteFile( resultFilePath )
    }
    Left( Unit )
  }
}
