/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental

import org.junit.Test
import kotlin.coroutines.experimental.*
import kotlin.test.*

class CoroutineContextThreadLocalTest : TestBase() {
    @Test
    fun testExample() = runTest {
        val exceptionHandler = coroutineContext[CoroutineExceptionHandler]!!
        val mainDispatcher = coroutineContext[ContinuationInterceptor]!!
        val mainThread = Thread.currentThread()
        val element = MyElement(MyData())
        assertNull(myThreadLocal.get())
        val job = launch(element + exceptionHandler) {
            assertTrue(mainThread != Thread.currentThread())
            assertSame(element, coroutineContext[MyElement])
            assertSame(element, myThreadLocal.get())
            withContext(mainDispatcher) {
                assertSame(mainThread, Thread.currentThread())
                assertSame(element, coroutineContext[MyElement])
                assertSame(element, myThreadLocal.get())
            }
            assertTrue(mainThread != Thread.currentThread())
            assertSame(element, coroutineContext[MyElement])
            assertSame(element, myThreadLocal.get())
        }
        assertNull(myThreadLocal.get())
        job.join()
        assertNull(myThreadLocal.get())
    }

    @Test
    fun testUndispatched()= runTest {
        val exceptionHandler = coroutineContext[CoroutineExceptionHandler]!!
        val element = MyElement(MyData())
        val job = launch(
            context = DefaultDispatcher + exceptionHandler + element,
            start = CoroutineStart.UNDISPATCHED
        ) {
            assertSame(element, myThreadLocal.get())
            yield()
            assertSame(element, myThreadLocal.get())
        }
        assertNull(myThreadLocal.get())
        job.join()
        assertNull(myThreadLocal.get())
    }
}

class MyData

// declare custom coroutine context element, storing some custom data
class MyElement(val data: MyData) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<MyElement>
}

// declare thread local variable
private val myThreadLocal = ThreadLocal<MyElement?>()

// declare extension point implementation
class MyCoroutineContextThreadLocal : CoroutineContextThreadLocal<MyElement?> {
    // this is invoked before coroutine is resumed on current thread
    override fun updateThreadContext(context: CoroutineContext): MyElement? {
        val oldValue = myThreadLocal.get()
        myThreadLocal.set(context[MyElement])
        return oldValue
    }

    // this is invoked after coroutine has suspended on current thread
    override fun restoreThreadContext(context: CoroutineContext, oldValue: MyElement?) {
        myThreadLocal.set(oldValue)
    }
}
