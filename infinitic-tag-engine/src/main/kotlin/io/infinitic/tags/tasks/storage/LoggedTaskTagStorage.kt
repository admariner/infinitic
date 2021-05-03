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

package io.infinitic.tags.tasks.storage

import io.infinitic.common.data.MessageId
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import org.jetbrains.annotations.TestOnly
import org.slf4j.LoggerFactory

class LoggedTaskTagStorage(
    val storage: TaskTagStorage
) : TaskTagStorage {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getLastMessageId(tag: TaskTag, taskName: TaskName): MessageId? {
        val messageId = storage.getLastMessageId(tag, taskName)
        logger.debug("tag {} - name {} - getLastMessageId {}", tag, taskName, messageId)

        return messageId
    }

    override suspend fun setLastMessageId(tag: TaskTag, taskName: TaskName, messageId: MessageId) {
        logger.debug("tag {} - name {} - setLastMessageId {}", tag, taskName, messageId)
        storage.setLastMessageId(tag, taskName, messageId)
    }

    override suspend fun getTaskIds(tag: TaskTag, taskName: TaskName): Set<TaskId> {
        val taskIds = storage.getTaskIds(tag, taskName)
        logger.debug("tag {} - taskName {} - getTaskIds {}", tag, taskName, taskIds)

        return taskIds
    }

    override suspend fun addTaskId(tag: TaskTag, taskName: TaskName, taskId: TaskId) {
        logger.debug("tag {} - name {} - addTaskId {}", tag, taskName, taskId)
        storage.addTaskId(tag, taskName, taskId)
    }

    override suspend fun removeTaskId(tag: TaskTag, taskName: TaskName, taskId: TaskId) {
        logger.debug("tag {} - name {} - removeTaskId {}", tag, taskName, taskId)
        storage.removeTaskId(tag, taskName, taskId)
    }

    @TestOnly
    override fun flush() {
        logger.debug("flush()")
        storage.flush()
    }
}