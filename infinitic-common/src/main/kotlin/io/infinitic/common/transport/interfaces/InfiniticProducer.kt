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
package io.infinitic.common.transport.interfaces

import io.infinitic.common.data.MillisDuration
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.events.messages.ServiceExecutorEventMessage
import io.infinitic.common.tasks.executors.messages.ServiceExecutorMessage
import io.infinitic.common.transport.Topic
import io.infinitic.common.transport.forWorkflow
import io.infinitic.common.transport.withoutDelay

interface InfiniticProducer {

  val emitterName: EmitterName

  suspend fun <T : Message> internalSendTo(
    message: T,
    topic: Topic<out T>,
    after: MillisDuration = MillisDuration(0),
  )

  /**
   * Sends a message to the specified topic asynchronously.
   *
   * @param topic the topic to send the message to
   * @param after the delay before consuming the message
   * @return a CompletableFuture that completes when the message has been sent
   */
  suspend fun <T : Message> T.sendTo(
    topic: Topic<out Message>,
    after: MillisDuration = MillisDuration(0)
  ) {
    // Switch to workflow-related topics for workflowTasks
    val t = when (this) {
      is ServiceExecutorMessage -> if (isWorkflowTask()) topic.forWorkflow else topic
      is ServiceExecutorEventMessage -> if (isWorkflowTask()) topic.forWorkflow else topic
      else -> topic
    }

    return when {
      after <= 0 -> internalSendTo(this, t.withoutDelay)
      else -> internalSendTo(this, t, after)
    }
  }
}
