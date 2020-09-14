package io.infinitic.taskManager.engine.pulsar.functions

import io.infinitic.storage.pulsar.PulsarFunctionStorage
import io.infinitic.taskManager.common.avro.AvroConverter
import io.infinitic.taskManager.dispatcher.pulsar.PulsarDispatcher
import io.infinitic.taskManager.engine.dispatcher.Dispatcher
import io.infinitic.taskManager.engine.engines.MonitoringPerName
import io.infinitic.taskManager.engine.storage.AvroKeyValueStateStorage
import io.infinitic.taskManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.functions.api.Context
import org.apache.pulsar.functions.api.Function

class MonitoringPerNamePulsarFunction : Function<AvroEnvelopeForMonitoringPerName, Void> {

    var monitoring = MonitoringPerName()

    override fun process(input: AvroEnvelopeForMonitoringPerName, context: Context?): Void? = runBlocking {
        val ctx = context ?: throw NullPointerException("Null Context received")

        val message = AvroConverter.fromMonitoringPerName(input)

        try {
            monitoring.logger = ctx.logger
            monitoring.storage = AvroKeyValueStateStorage(PulsarFunctionStorage(ctx))
            monitoring.dispatcher = Dispatcher(PulsarDispatcher.forPulsarFunctionContext(ctx))

            monitoring.handle(message)
        } catch (e: Exception) {
            ctx.logger.error("Error:%s for message:%s", e, input)
            throw e
        }

        null
    }
}
