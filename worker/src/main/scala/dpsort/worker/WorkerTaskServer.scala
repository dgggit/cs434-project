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

package dpsort.worker

import scala.concurrent.{ExecutionContext, Future}

import org.apache.logging.log4j.scala.Logging

import dpsort.core.execution.BaseTask
import dpsort.core.utils.SerializationUtils
import dpsort.core.network.{ResponseMsg, ServerContext, ServerInterface, TaskMsg, WorkerTaskServiceGrpc}
import dpsort.core.utils.SerializationUtils.deserializeByteStringToObject
import dpsort.worker.WorkerConf._




object WorkerTaskServer extends ServerInterface {

  private val port = get("dpsort.worker.port").toInt

  val server : ServerContext = new ServerContext(
    WorkerTaskServiceGrpc.bindService(new WorkerTaskServiceImpl, ExecutionContext.global),
    "WorkerTaskServer",
    port
  )

}

private class WorkerTaskServiceImpl extends WorkerTaskServiceGrpc.WorkerTaskService with Logging {

  override def requestTask(request: TaskMsg): Future[ResponseMsg] = {
    val task: BaseTask = deserializeByteStringToObject[BaseTask]( request.serializedTaskObject )
    val response: ResponseMsg = TaskManager.taskRequestHandler( task )
    Future.successful( response )
  }

}