package com.zenaton.jobManager.avroEngines

import com.zenaton.jobManager.avroConverter.AvroConverter
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryDispatcher
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryStorage
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryWorker.Status
import com.zenaton.jobManager.avroEngines.inMemory.InMemoryWorkerTask
import com.zenaton.jobManager.data.AvroJobStatus
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import org.slf4j.Logger

private val logger = mockk<Logger>(relaxed = true)
private val jobEngine = AvroJobEngine()
private val monitoringPerName = AvroMonitoringPerName()
private val monitoringGlobal = AvroMonitoringGlobal()
private val worker = InMemoryWorkerTask()
private val dispatcher = InMemoryDispatcher(jobEngine, monitoringPerName, monitoringGlobal, worker)
private val storage = InMemoryStorage()

class AvroEngineTests : StringSpec({
    beforeTest {
        worker.jobA = mockk()
        every { worker.jobA.handle() } just Runs
        storage.init()
    }

    "Job succeeds at first try" {
        // job will succeed
        worker.behavior = { Status.SUCCESS }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
        // checks number of job processing
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job succeeds at 4th try" {
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob -> if (job.jobAttemptRetry < 3) Status.FAIL_WITH_RETRY else Status.SUCCESS }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
        // checks number of job processing
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job fails" {
        // job will succeed only at the 4th try
        worker.behavior = { Status.FAIL_WITHOUT_RETRY }
        // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processing
        verify(exactly = 1) {
            worker.jobA.handle()
        }
    }

    "Job fails after 4 trys " {
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob ->
            if (job.jobAttemptIndex == 0)
                if (job.jobAttemptRetry < 3) Status.FAIL_WITH_RETRY else Status.FAIL_WITHOUT_RETRY
            else
                Status.SUCCESS
        } // run system
        val dispatch = getAvroDispatchJob()
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId]?.jobStatus shouldBe AvroJobStatus.RUNNING_ERROR
        // checks number of job processing
        verify(exactly = 4) {
            worker.jobA.handle()
        }
    }

    "Job succeeds after manual retry" {
        // job will succeed only at the 4th try
        worker.behavior = { job: AvroRunJob ->
            if (job.jobAttemptIndex == 0)
                if (job.jobAttemptRetry < 3) Status.FAIL_WITH_RETRY else Status.FAIL_WITHOUT_RETRY
            else
                Status.SUCCESS
        }
        // run system
        val dispatch = getAvroDispatchJob()
        val retry = getAvroRetryJob(dispatch.jobId)
        coroutineScope {
            dispatcher.scope = this
            dispatcher.toJobEngine(dispatch)
            delay(100)
            dispatcher.toJobEngine(retry)
        }
        // check that job is completed
        storage.jobEngineStore[dispatch.jobId] shouldBe null
    }
}) {
    init {
        jobEngine.avroStorage = storage
        jobEngine.avroDispatcher = dispatcher
        jobEngine.logger = logger

        monitoringPerName.avroStorage = storage
        monitoringPerName.avroDispatcher = dispatcher
        monitoringPerName.logger = logger

        monitoringGlobal.avroStorage = storage
        monitoringGlobal.logger = logger

        worker.avroDispatcher = dispatcher
    }
}

private fun getAvroDispatchJob() = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroDispatchJob::class, mapOf("jobName" to "JobA"))
)

private fun getAvroRetryJob(id: String) = AvroConverter.addEnvelopeToJobEngineMessage(
    TestFactory.random(AvroRetryJob::class, mapOf("jobId" to id))
)