package io.infinitic.taskManager.engine.engines

import io.infinitic.taskManager.common.data.TaskStatus
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.common.messages.TaskCreated
import io.infinitic.taskManager.common.messages.TaskStatusUpdated
import io.infinitic.taskManager.common.states.MonitoringPerNameState
import io.infinitic.taskManager.engine.storages.MonitoringPerNameStorage
import io.infinitic.taskManager.engine.utils.TestFactory
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verifyAll
import org.slf4j.Logger

class MonitoringPerNameTests : ShouldSpec({
    context("TaskMetrics.handle") {
        should("should update TaskMetricsState when receiving TaskStatusUpdate message") {
            val storage = mockk<MonitoringPerNameStorage>()
            val dispatcher = mockk<Dispatcher>()
            val logger = mockk<Logger>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to TaskStatus.RUNNING_OK,
                    "newStatus" to TaskStatus.RUNNING_ERROR
                )
            )
            val stateIn = TestFactory.random(MonitoringPerNameState::class, mapOf("taskName" to msg.taskName))
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.taskName) } returns stateIn
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs

            val monitoringPerName = MonitoringPerName()
            monitoringPerName.logger = logger
            monitoringPerName.storage = storage
            monitoringPerName.dispatcher = dispatcher

            monitoringPerName.handle(msg)

            val stateOut = stateOutSlot.captured
            verifyAll {
                storage.getState(msg.taskName)
                storage.updateState(msg.taskName, stateOut, stateIn)
            }
            stateOut.runningErrorCount shouldBe stateIn.runningErrorCount + 1
            stateOut.runningOkCount shouldBe stateIn.runningOkCount - 1
        }

        should("dispatch message when discovering a new task type") {
            val storage = mockk<MonitoringPerNameStorage>()
            val dispatcher = mockk<Dispatcher>()
            val logger = mockk<Logger>()
            val msg = TestFactory.random(
                TaskStatusUpdated::class,
                mapOf(
                    "oldStatus" to null,
                    "newStatus" to TaskStatus.RUNNING_OK
                )
            )
            val stateOutSlot = slot<MonitoringPerNameState>()
            every { storage.getState(msg.taskName) } returns null
            every { storage.updateState(msg.taskName, capture(stateOutSlot), any()) } just runs
            coEvery { dispatcher.toMonitoringGlobal(any<TaskCreated>()) } just runs

            val monitoringPerName = MonitoringPerName()
            monitoringPerName.logger = logger
            monitoringPerName.storage = storage
            monitoringPerName.dispatcher = dispatcher
            // when
            monitoringPerName.handle(msg)
            // then
            val stateOut = stateOutSlot.captured
            coVerifyAll {
                storage.getState(msg.taskName)
                storage.updateState(msg.taskName, stateOut, null)
                dispatcher.toMonitoringGlobal(ofType<TaskCreated>())
            }
        }
    }
})
