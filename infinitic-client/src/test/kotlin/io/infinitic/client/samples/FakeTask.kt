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

package io.infinitic.client.samples

import io.infinitic.annotations.Name
import io.infinitic.common.tasks.data.TaskId

internal interface FakeTaskParent {
    fun parent(): String

    @Name("bar")
    fun annotated(): String
}

internal interface FakeTask : FakeTaskParent {
    fun m1()
    fun m1(i: Int): String
    fun m1(str: String?): Any?
    fun m1(p1: Int, p2: String): String
    fun m1(id: FakeInterface): TaskId
    fun m2(): Boolean
    suspend fun suspendedMethod()
}

@Name("foo")
internal interface FooTask : FakeTaskParent {
    @Name("bar")
    fun m()
}
