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
package io.infinitic.dashboard.panels.infrastructure

import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.dashboard.Infinitic
import io.infinitic.dashboard.panels.infrastructure.requests.Loading
import org.apache.pulsar.common.policies.data.PartitionedTopicStats
import java.time.Instant

data class AllWorkflowsState(
  override val names: JobNames = Loading(),
  override val stats: JobStats = mapOf(),
  val isLoading: Boolean = isLoading(names, stats),
  val lastUpdatedAt: Instant = lastUpdatedAt(names, stats)
) : AllJobsState(names, stats) {

  override fun create(names: JobNames, stats: JobStats) =
      AllWorkflowsState(names = names, stats = stats)

  override suspend fun getNames() = Infinitic.pulsarResources.getWorkflowNames()

  override suspend fun getPartitionedStats(name: String): Result<PartitionedTopicStats?> {
    val topic =
        with(Infinitic.pulsarResources) { WorkflowExecutorTopic.fullName(name) }

    return Infinitic.pulsarResources.admin.getPartitionedTopicStats(topic)
  }
}
