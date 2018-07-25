/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental

import kotlin.coroutines.experimental.*

/**
 * An extension point to define elements in [CoroutineContext] that are installed into thread local
 * variables every time the coroutine from the specified context in resumed on a thread.
 *
 * Implementations on this interface are looked up via [java.util.ServiceLoader].
 *
 * Example usage looks like this:
 *
 * ```
 * // declare custom coroutine context element, storing some custom data
 * class MyElement(val data: MyData) : AbstractCoroutineContextElement(Key) {
 *     companion object Key : CoroutineContext.Key<MyElement>
 * }
 *
 * // declare thread local variable
 * private val myThreadLocal = ThreadLocal<MyElement?>()
 *
 * // declare extension point implementation
 * class MyCoroutineContextThreadLocal : CoroutineContextThreadLocal<MyElement?> {
 *     // this is invoked before coroutine is resumed on current thread
 *     override fun updateThreadContext(context: CoroutineContext): MyElement? {
 *         val oldValue = myThreadLocal.get()
 *         myThreadLocal.set(context[MyElement])
 *         return oldValue
 *     }
 *
 *     // this is invoked after coroutine has suspended on current thread
 *     override fun restoreThreadContext(context: CoroutineContext, oldValue: MyElement?) {
 *         myThreadLocal.set(oldValue)
 *     }
 * }
 * ```
 *
 * Now, `MyCoroutineContextThreadLocal` fully qualified class named shall be registered via
 * `META-INF/services/kotlinx.coroutines.experimental.CoroutineContextThreadLocal` file.
 */
public interface CoroutineContextThreadLocal<T> {
    /**
     * Updates context of the current thread.
     * This function is invoked before the coroutine in the specified [context] is resumed in the current thread.
     * The result of this function is the old value that will be passed to [restoreThreadContext].
     */
    public fun updateThreadContext(context: CoroutineContext): T

    /**
     * Restores context of the current thread.
     * This function is invoked after the coroutine in the specified [context] is suspended in the current thread.
     * The value of [oldValue] is the result of the previous invocation of [updateThreadContext].
     */
    public fun restoreThreadContext(context: CoroutineContext, oldValue: T)
}

/**
 * This class is used when multiple [CoroutineContextThreadLocal] are installed. 
 */
internal class CoroutineContextThreadLocalList(
    private val impls: Array<CoroutineContextThreadLocal<Any?>>
) : CoroutineContextThreadLocal<Any?> {
    init {
        require(impls.size > 1)
    }

    private val threadLocalStack = ThreadLocal<ArrayList<Any?>?>()

    override fun updateThreadContext(context: CoroutineContext): Any? {
        val stack = threadLocalStack.get() ?: ArrayList<Any?>().also {
            threadLocalStack.set(it)
        }
        val lastIndex = impls.lastIndex
        for (i in 0 until lastIndex) {
            stack.add(impls[i].updateThreadContext(context))
        }
        return impls[lastIndex].updateThreadContext(context)
    }

    override fun restoreThreadContext(context: CoroutineContext, oldValue: Any?) {
        val stack = threadLocalStack.get()!! // must be there
        val lastIndex = impls.lastIndex
        impls[lastIndex].restoreThreadContext(context, oldValue)
        for (i in lastIndex - 1 downTo 0) {
            impls[i].restoreThreadContext(context, stack.removeAt(stack.lastIndex))
        }
    }
}