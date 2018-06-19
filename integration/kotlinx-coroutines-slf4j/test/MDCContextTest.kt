/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental.slf4j

import kotlinx.coroutines.experimental.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.slf4j.MDC
import kotlin.coroutines.experimental.*
import kotlin.test.assertEquals

class MDCContextTest : TestBase() {
    @Before
    fun setUp() {
        MDC.clear()
    }

    @After
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun mdcContextIsNotPassedByDefaultBetweenCoroutines() = runTest {
        expect(1)
        MDC.put("myKey", "myValue")

        launch {
            assertEquals(null, MDC.get("myKey"))
            expect(2)
        }.join()

        finish(3)
    }

    @Test
    fun mdcContextCanBePassedBetweenCoroutines() = runTest {
        expect(1)
        MDC.put("myKey", "myValue")

        launch(MDCContext()) {
            assertEquals("myValue", MDC.get("myKey"))
            expect(2)
        }.join()

        finish(3)
    }

    @Test
    fun mdcContextNotPassedWhileOnMainThread() {
        MDC.put("myKey", "myValue")

        runBlocking {
            assertEquals(null, MDC.get("myKey"))
        }
    }

    @Test
    fun mdcContextCanBePassedWhileOnMainThread() {
        MDC.put("myKey", "myValue")

        runBlocking(MDCContext()) {
            assertEquals("myValue", MDC.get("myKey"))
        }
    }

    @Test
    fun mdcContextNeededWithOtherContext() {
        MDC.put("myKey", "myValue")

        runBlocking(MDCContext()) {
            assertEquals("myValue", MDC.get("myKey"))
        }
    }

    @Test
    fun mdcContextMayBeEmpty() {
        runBlocking(MDCContext()) {
            assertEquals(null, MDC.get("myKey"))
        }
    }

    @Test
    fun mdcContextWithContext() = runTest {
        MDC.put("myKey", "myValue")
        val mainDispatcher = kotlin.coroutines.experimental.coroutineContext[ContinuationInterceptor]!!
        withContext(DefaultDispatcher + MDCContext()) {
            assertEquals("myValue", MDC.get("myKey"))
            withContext(mainDispatcher) {
                assertEquals("myValue", MDC.get("myKey"))
            }
        }
    }
}