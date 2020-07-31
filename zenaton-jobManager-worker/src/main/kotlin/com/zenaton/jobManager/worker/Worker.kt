package com.zenaton.jobManager.worker

import com.zenaton.common.data.SerializedData
import com.zenaton.jobManager.common.data.JobAttemptError
import com.zenaton.jobManager.common.data.JobInput
import com.zenaton.jobManager.common.data.JobOutput
import com.zenaton.jobManager.common.messages.ForWorkerMessage
import com.zenaton.jobManager.common.messages.JobAttemptCompleted
import com.zenaton.jobManager.common.messages.JobAttemptFailed
import com.zenaton.jobManager.common.messages.JobAttemptStarted
import com.zenaton.jobManager.common.messages.RunJob
import com.zenaton.jobManager.data.AvroSerializedDataType
import org.apache.avro.specific.SpecificRecordBase
import java.lang.reflect.Method
import java.security.InvalidParameterException

class Worker {
    lateinit var dispatcher: Dispatcher

    companion object {
        const val METHOD_DIVIDER = "::"
        const val METHOD_DEFAULT = "handle"
        const val META_PARAMETER_TYPES = "javaParameterTypes"
    }

    private val registeredJobs = mutableMapOf<String, Any>()

    /**
     * With this method, user can register a job instance to use for a given name
     */
    fun register(name: String, job: Any): Worker {
        if (name.contains(METHOD_DIVIDER)) throw InvalidParameterException("Job name \"$name\" must not contain the \"$METHOD_DIVIDER\" divider")

        registeredJobs[name] = job

        return this
    }

    fun handle(msg: ForWorkerMessage) {
        when (msg) {
            is RunJob -> {
                sendJobStarted(msg)
                try {
                    sendJobCompleted(msg, run(msg))
                } catch (e: Exception) {
                    sendJobFailed(msg, e)
                }
            }
        }
    }

    private fun run(msg: RunJob): Any? {
        val (jobName, methodName) = getClassAndMethodNames(msg)
        val job = getJob(jobName)
        val parameterTypes = getMetaParameterTypes(msg)
        val method = getMethod(job, methodName, msg.jobInput.input.size, parameterTypes)
        val parameters = getParameters(msg.jobInput, parameterTypes ?: method.parameterTypes)

        return method.invoke(job, *parameters)
    }

    private fun getClassAndMethodNames(msg: RunJob): List<String> {
        val parts = msg.jobName.name.split(METHOD_DIVIDER)
        return when (parts.size) {
            1 -> parts + METHOD_DEFAULT
            2 -> parts
            else -> throw InvalidParameterException("Job name \"$msg.jobName.name\" must not contain the $METHOD_DIVIDER divider more than once")
        }
    }

    private fun getJob(name: String): Any {
        // return registered instance if any
        if (registeredJobs.containsKey(name)) return registeredJobs[name]!!

        // if no instance is registered, try to instantiate this job
        val klass = getClass(name)

        return try {
            klass.newInstance()
        } catch (e: Exception) {
            throw InstantiationError("Impossible to instantiate job \"$name\" - please use \"register\" method to provide an instance")
        }
    }

    private fun getMetaParameterTypes(msg: RunJob) = msg.jobMeta.meta[META_PARAMETER_TYPES]?.fromJson<List<String>>()?.map { getClass(it) }?.toTypedArray()

    private fun getClass(name: String) = when (name) {
        "bytes" -> Byte::class.java
        "short" -> Short::class.java
        "int" -> Int::class.java
        "long" -> Long::class.java
        "float" -> Float::class.java
        "double" -> Double::class.java
        "boolean" -> Boolean::class.java
        "char" -> Character::class.java
        else ->
            try {
                Class.forName(name)
            } catch (e: ClassNotFoundException) {
                throw ClassNotFoundException("Impossible to find a Class associated to job \"$name\" - please use \"register\" method to provide an instance")
            }
    }

    private fun getMethod(job: Any, methodName: String, parameterCount: Int, parameterTypes: Array<Class<*>>?): Method {
        // Case where parameter types have been provided
        if (parameterTypes != null) return job::class.java.getMethod(methodName, *parameterTypes)

        // if not, hopefully there is only one method with this name
        val methods = job::class.javaObjectType.methods.filter { it.name == methodName && it.parameterCount == parameterCount }
        if (methods.size != 1) throw Exception("Unable to decide which method \"$methodName\" to use in \"${job::class}\" job")

        return methods[0]
    }

    private fun getParameters(input: JobInput, parameterTypes: Array<Class<*>>): Array<Any?> {
        return input.input.mapIndexed {
            index, serializedData ->
            when (serializedData.type) {
                AvroSerializedDataType.NULL -> null
                AvroSerializedDataType.BYTES -> serializedData.bytes
                AvroSerializedDataType.JSON -> serializedData.fromJson(parameterTypes[index])
                AvroSerializedDataType.AVRO -> serializedData.fromAvro(parameterTypes[index] as Class<out SpecificRecordBase>)
                else -> throw Exception("Can't deserialize data with CUSTOM serialization")
            }
        }.toTypedArray()
    }

    private fun sendJobStarted(msg: RunJob) {
        val jobAttemptStarted = JobAttemptStarted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex
        )

        dispatcher.toJobEngine(jobAttemptStarted)
    }

    private fun sendJobFailed(msg: RunJob, error: Exception, delay: Float? = null) {
        val jobAttemptFailed = JobAttemptFailed(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobAttemptDelayBeforeRetry = delay,
            jobAttemptError = JobAttemptError(error)
        )

        dispatcher.toJobEngine(jobAttemptFailed)
    }

    private fun sendJobCompleted(msg: RunJob, output: Any?) {
        val jobAttemptCompleted = JobAttemptCompleted(
            jobId = msg.jobId,
            jobAttemptId = msg.jobAttemptId,
            jobAttemptRetry = msg.jobAttemptRetry,
            jobAttemptIndex = msg.jobAttemptIndex,
            jobOutput = JobOutput(SerializedData.from(output))
        )

        dispatcher.toJobEngine(jobAttemptCompleted)
    }
}