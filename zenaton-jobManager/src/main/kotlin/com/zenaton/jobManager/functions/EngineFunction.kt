package com.zenaton.jobManager.functions

import com.zenaton.jobManager.avro.AvroConverter
import com.zenaton.jobManager.dispatcher.Dispatcher
import com.zenaton.jobManager.engine.Engine
import com.zenaton.jobManager.engine.EngineStorage
import com.zenaton.jobManager.interfaces.AvroDispatcher
import com.zenaton.jobManager.interfaces.AvroStorage
import com.zenaton.jobManager.messages.envelopes.AvroForEngineMessage
import org.slf4j.Logger

class EngineFunction {
    lateinit var logger: Logger
    lateinit var avroStorage: AvroStorage
    lateinit var avroDispatcher: AvroDispatcher

    var engine = Engine()

    fun handle(input: AvroForEngineMessage) {
        engine.logger = logger
        engine.storage = EngineStorage(avroStorage)
        engine.dispatcher = Dispatcher(avroDispatcher)

        engine.handle(AvroConverter.fromEngine(input))
    }
}