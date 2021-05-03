/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.commands

import io.infinitic.common.data.Data
import io.infinitic.common.serDe.SerializedData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable(with = CommandOutputSerializer::class)
data class CommandReturnValue(override val serializedData: SerializedData) : Data(serializedData) {
    companion object {
        fun now() = CommandReturnValue(SerializedData.from(Instant.now()))
        fun from(data: Any?) = CommandReturnValue(SerializedData.from(data))
    }
}

object CommandOutputSerializer : KSerializer<CommandReturnValue> {
    override val descriptor: SerialDescriptor = SerializedData.serializer().descriptor
    override fun serialize(encoder: Encoder, value: CommandReturnValue) {
        SerializedData.serializer().serialize(encoder, value.serializedData)
    }
    override fun deserialize(decoder: Decoder) =
        CommandReturnValue(SerializedData.serializer().deserialize(decoder))
}