package io.infinitic.taskManager.client

import io.infinitic.taskManager.common.exceptions.MultipleMethodCallsAtDispatch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyHandler : InvocationHandler {
    var method: Method? = null
    lateinit var args: Array<out Any>

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        // invoke should called only once per ProxyHandler instance
        if (this.method != null) throw MultipleMethodCallsAtDispatch(method.declaringClass.name, this.method!!.name, method.name)

        // methods and args are stored for later use
        this.method = method
        this.args = args ?: arrayOf()

        // explicit cast needed for all primitive types
        return when (method.returnType.name) {
            "long" -> 0L
            "int" -> 0.toInt()
            "short" -> 0.toShort()
            "byte" -> 0.toByte()
            "double" -> 0.toDouble()
            "float" -> 0.toFloat()
            "char" -> 0.toChar()
            "boolean" -> false
            else -> null
        }
    }
}
