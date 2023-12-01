package com.linkedplanet.ktor.client.logging

import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

suspend fun <T> withMdc(vararg infos: Pair<String, Any?>, func: suspend () -> T): T =
    withMdc(MDC.getCopyOfContextMap() ?: emptyMap(), infos.toMap(), func)

private suspend fun <T> withMdc(oldState: Map<String, String>, newState: Map<String, Any?>, func: suspend () -> T): T {
    newState.entries.forEach { (key, value) ->
        MDC.put(key, value.toString())
    }
    return try {
        withContext(MDCContext()) {
            func()
        }
    } finally {
        // the MDC context does not reliably capture the old context before switching into the new context
        // this leads to values getting lost when restoring the old context, so we take care of it ourselves
        oldState.entries.forEach { (key, value) ->
            MDC.put(key, value)
        }
    }
}
