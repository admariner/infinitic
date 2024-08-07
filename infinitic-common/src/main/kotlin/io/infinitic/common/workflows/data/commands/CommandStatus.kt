/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.workflows.data.commands

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.tasks.executors.errors.DeferredCanceledError
import io.infinitic.common.tasks.executors.errors.DeferredFailedError
import io.infinitic.common.tasks.executors.errors.DeferredTimedOutError
import io.infinitic.common.tasks.executors.errors.DeferredUnknownError
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@klass")
sealed class CommandStatus {
  /** A command is terminated if canceled or completed, failed or timedOut are transient state */
  @JsonIgnore
  fun isTerminated() = this is Completed || this is Canceled

  @Serializable
  @SerialName("CommandStatus.Ongoing")
  object Ongoing : CommandStatus() {
    override fun equals(other: Any?) = javaClass == other?.javaClass

    override fun toString(): String = Ongoing::class.java.name
  }

  /**
   * @param deferredUnknownError the error describing the error on an unknown deferred
   * @param unknowingWorkflowTaskIndex the value of WorkflowTaskIndex at unknown
   */
  @Serializable
  @SerialName("CommandStatus.Unknown")
  data class Unknown(
    @SerialName("unknownDeferredError")
    val deferredUnknownError: DeferredUnknownError,
    val unknowingWorkflowTaskIndex: WorkflowTaskIndex
  ) : CommandStatus()

  /**
   * @param deferredCanceledError the error describing the cancellation
   * @param cancellationWorkflowTaskIndex the value of WorkflowTaskIndex at cancellation
   */
  @Serializable
  @SerialName("CommandStatus.Canceled")
  data class Canceled(
    @SerialName("canceledDeferredError")
    val deferredCanceledError: DeferredCanceledError,
    val cancellationWorkflowTaskIndex: WorkflowTaskIndex
  ) : CommandStatus()

  /**
   * @param deferredFailedError the error describing the failure
   * @param failureWorkflowTaskIndex the value of WorkflowTaskIndex at failure
   */
  @Serializable
  @SerialName("CommandStatus.Failed")
  data class Failed(
    @SerialName("failedDeferredError")
    val deferredFailedError: DeferredFailedError,
    val failureWorkflowTaskIndex: WorkflowTaskIndex
  ) : CommandStatus()

  /**
   * @param deferredTimedOutError the error describing the timeout
   * @param timeoutWorkflowTaskIndex the value of WorkflowTaskIndex at timeout
   */
  @Serializable
  @SerialName("CommandStatus.TimedOut")
  data class TimedOut(
    val deferredTimedOutError: DeferredTimedOutError,
    val timeoutWorkflowTaskIndex: WorkflowTaskIndex
  ) : CommandStatus()

  /**
   * @param returnValue the return value of the completed command
   * @param completionWorkflowTaskIndex the value of WorkflowTaskIndex at completion
   */
  @Serializable
  @SerialName("CommandStatus.Completed")
  data class Completed(
    @AvroDefault("0") val returnIndex: Int = 0,
    val returnValue: MethodReturnValue,
    val completionWorkflowTaskIndex: WorkflowTaskIndex,
    @AvroDefault(Avro.NULL) val signalId: SignalId? = null
  ) : CommandStatus()
}
