package com.zenaton.pulsar.workflows.serializers

import com.zenaton.engine.workflows.messages.WorkflowMessage
import com.zenaton.pulsar.workflows.PulsarMessage

interface MessageSerDeInterface {
    fun fromPulsar(input: PulsarMessage): WorkflowMessage
    fun toPulsar(msg: WorkflowMessage): PulsarMessage
    fun toJson(msg: Any): String
}