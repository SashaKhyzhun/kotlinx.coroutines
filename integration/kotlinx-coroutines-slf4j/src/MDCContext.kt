/*
 * Copyright 2016-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.coroutines.experimental.slf4j

import kotlinx.coroutines.experimental.*
import org.slf4j.MDC
import kotlin.coroutines.experimental.AbstractCoroutineContextElement
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * The value of [MDC] context map.
 * See [MDC.getCopyOfContextMap].
 */
public typealias MDCContextMap = Map<String, String>?

/**
 * [MDC] context element for [CoroutineContext].
 *
 * Example:
 *
 * ```
 * MDC.put("kotlin", "rocks") // put a value into the MDC context
 *
 * launch(MDCContext()) {
 *     logger.info { "..." }   // the MDC context contains the mapping here
 * }
 * ```
 */
public class MDCContext(
    /**
     * The value of [MDC] context map.
     */
    public val contextMap: MDCContextMap = MDC.getCopyOfContextMap()
) : AbstractCoroutineContextElement(Key) {
    /**
     * Key of [MDCContext] in [CoroutineContext].
     */
    companion object Key : CoroutineContext.Key<MDCContext>
}

internal class MDCContextThreadLocal : CoroutineContextThreadLocal<MDCContextMap> {
    override fun updateThreadContext(context: CoroutineContext): MDCContextMap {
        val oldValue = MDC.getCopyOfContextMap()
        val contextMap = context[MDCContext]?.contextMap
        if (contextMap == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(contextMap)
        }
        return oldValue
    }

    override fun restoreThreadContext(context: CoroutineContext, oldValue: MDCContextMap) {
        if (oldValue == null) {
            MDC.clear()
        } else {
            MDC.setContextMap(oldValue)
        }
    }
}
