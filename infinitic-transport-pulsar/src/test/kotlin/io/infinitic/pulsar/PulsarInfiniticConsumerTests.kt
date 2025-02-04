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

package io.infinitic.pulsar

import io.github.oshai.kotlinlogging.KotlinLogging
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.fixtures.later
import io.infinitic.common.messages.Envelope
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.transport.ClientTopic
import io.infinitic.common.transport.MainSubscription
import io.infinitic.common.transport.ServiceExecutorEventTopic
import io.infinitic.common.transport.ServiceExecutorTopic
import io.infinitic.common.transport.ServiceTagEngineTopic
import io.infinitic.common.transport.WorkflowExecutorEventTopic
import io.infinitic.common.transport.WorkflowExecutorTopic
import io.infinitic.common.transport.WorkflowStateCmdTopic
import io.infinitic.common.transport.WorkflowStateEngineTopic
import io.infinitic.common.transport.WorkflowStateEventTopic
import io.infinitic.common.transport.WorkflowStateTimerTopic
import io.infinitic.common.transport.WorkflowTagEngineTopic
import io.infinitic.common.transport.consumers.startProcessingWithKey
import io.infinitic.common.transport.logged.LoggerWithCounter
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.pulsar.client.InfiniticPulsarClient
import io.infinitic.pulsar.config.PulsarConfig
import io.infinitic.pulsar.config.policies.PoliciesConfig
import io.infinitic.pulsar.resources.PulsarResources
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import net.bytebuddy.utility.RandomString
import org.apache.pulsar.client.api.Schema
import java.util.concurrent.CompletableFuture
import org.apache.pulsar.client.api.Consumer as PulsarConsumer
import org.apache.pulsar.client.api.Message as PulsarMessage

class PulsarInfiniticConsumerTests : StringSpec(
    {
      val logger = LoggerWithCounter(KotlinLogging.logger("io.infinitic.tests"))
      val clientName = ClientName("clientTest")
      val workflowName = WorkflowName("workflowTest")
      val serviceName = ServiceName("serviceTest")

      val tenant = RandomString().nextString()
      val namespace = RandomString().nextString()

      val pulsarConfig = PulsarConfig(
          brokerServiceUrl = "pulsar://localhost:6650",
          webServiceUrl = "http://localhost:8080",
          tenant = tenant,
          namespace = namespace,
          allowedClusters = setOf(),
          adminRoles = setOf(),
          policies = PoliciesConfig(),
      )

      val pulsarResources = spyk(PulsarResources(pulsarConfig)) {
        coEvery { initTopicOnce(any(), any(), any()) } returns Result.success(Unit)
        coEvery { initDlqTopicOnce(any(), any(), any()) } returns Result.success(Unit)
      }

      val pulsarConsumer = mockk<PulsarConsumer<Envelope<out Message>>> {
        // delay here is to avoid the main loop in startConsuming to loop too quickly,
        // creating a huge amount of CompletableFuture and Mockk objects,
        // eventually leading to memory issues
        coEvery { receiveAsync() } coAnswers {
          delay(10)
          CompletableFuture<PulsarMessage<Envelope<out Message>>>()
        }
        every { consumerName } returns "consumerName"
      }

      val client = mockk<InfiniticPulsarClient> {
        every { newConsumer(any<Schema<Envelope<out Message>>>(), any(), any()) } returns
            Result.success(pulsarConsumer)
      }

      val consumerFactory =
          PulsarInfiniticConsumerFactory(client, pulsarConfig.consumer, pulsarResources)

      fun getScope(): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.IO)
        later(500) { scope.cancel() }
        return scope
      }

      "should init client-response topic before consuming it" {
        val name = "$clientName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(ClientTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/response:$name",
              isPartitioned = false,
              isTimer = false,
          )
        }
      }

      "should init workflow-tag topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowTagEngineTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-tag:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-cmd topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowStateCmdTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-cmd:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-engine topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowStateEngineTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-engine:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-delay topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowStateTimerTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 10,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-delay:$name",
              isPartitioned = true,
              isTimer = true,
          )
        }
      }

      "should init workflow-events topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowStateEventTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 10,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-task-executor topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowExecutorTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-executor:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init workflow-task-events topic before consuming it" {
        val name = "$workflowName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(WorkflowExecutorEventTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 10,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/workflow-task-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-tag topic before consuming it" {
        val name = "$serviceName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(ServiceTagEngineTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 1,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-tag:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-executor topic before consuming it" {
        val name = "$serviceName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(ServiceExecutorTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 10,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-executor:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }

      "should init task-events topic before consuming it" {
        val name = "$serviceName"

        val consumer = consumerFactory.newConsumer(
            subscription = MainSubscription(ServiceExecutorEventTopic),
            entity = name,
            batchReceivingConfig = null,
        )

        getScope().startProcessingWithKey(
            logger = logger,
            consumer = consumer,
            concurrency = 10,
            processor = { _, _ -> },
            beforeDlq = { _, _ -> },
        )

        coVerify {
          pulsarResources.initTopicOnce(
              "persistent://$tenant/$namespace/task-events:$name",
              isPartitioned = true,
              isTimer = false,
          )
        }
      }
    },
)
