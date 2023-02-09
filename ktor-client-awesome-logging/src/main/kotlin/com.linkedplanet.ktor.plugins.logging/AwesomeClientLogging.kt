/*
 * #%L
 * ktor-client-awesome-logging
 * %%
 * Copyright (C) 2023 linked-planet GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
@file:Suppress("unused")

package com.linkedplanet.ktor.plugins.logging

import io.ktor.client.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.observer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import java.nio.charset.Charset
import java.util.*

interface Logger {
    fun log(message: String, level: Level)

    companion object {
        val DEFAULT: Logger = object : Logger {
            private val delegate = LoggerFactory.getLogger(HttpClient::class.java)

            override fun log(message: String, level: Level) {
                when (level) {
                    Level.TRACE -> log(message, delegate::trace)
                    Level.DEBUG -> log(message, delegate::debug)
                    Level.INFO -> log(message, delegate::info)
                    Level.WARN -> log(message, delegate::warn)
                    Level.ERROR -> log(message, delegate::error)
                    else -> {
                        // Do nothing
                    }
                }
            }

            private fun log(message: String, logHandler: (String) -> Unit) {
                logHandler(message)
            }
        }
    }
}


private val traceIdKey = AttributeKey<String>("traceId")

@OptIn(InternalAPI::class)
val AwesomeClientLogging = createClientPlugin("AwesomeClientLogging", ::AwesomeClientLoggingConfig) {
    val config = pluginConfig

    on(SendingRequest) { request, content ->
        val body =
            if (!config.requestConfig.logBody) null
            else {
                when (content) {
                    is EmptyContent -> null
                    is TextContent -> String(content.bytes())
                    else -> "[request body unavailable]"
                }
            }
        logRequest(config, request, body)
    }

    onResponse { response ->
        val body =
            if (response.status.isSuccess() || !config.responseConfig.logBodyOnError) null
            else {
                val wrapped = response.call.wrapWithContent(response.content)
                wrapped.response.contentType()?.let { contentType ->
                    val charset = contentType.charset() ?: Charsets.UTF_8
                    wrapped.response.content.tryReadText(charset) ?: "[response body unavailable]"
                }
            }
        logResponse(config, response, body)
    }
}

private suspend fun logRequest(config: AwesomeClientLoggingConfig, request: HttpRequestBuilder, body: String? = null) {
    request.attributes.put(traceIdKey, UUID.randomUUID().toString())
    val traceId = request.attributes[traceIdKey]
    val requestUrl = Url(request.url)
    val from = "${request.method.value} $requestUrl"
    val logBody = body?.let { "${config.requestConfig.bodyDelimiter}${it}" } ?: ""
    val message = "${config.requestConfig.prefix}${from}${logBody}"
    withMdc(config.traceIdFieldName to traceId) {
        config.logger.log(message, config.requestConfig.level)
    }
}

private suspend fun logResponse(config: AwesomeClientLoggingConfig, response: HttpResponse, body: String? = null) {
    val traceId = response.call.attributes[traceIdKey]
    val from = "${response.call.request.method.value} ${response.call.request.url}"
    val statusCode = response.status.value
    val logBody = body?.let { "${config.responseConfig.bodyDelimiter}${it}" } ?: ""
    val message = "${config.responseConfig.prefix}${statusCode}: ${from}${logBody}"
    withMdc(config.traceIdFieldName to traceId) {
        val level =
            if (response.status.isSuccess()) config.responseConfig.levelSuccess
            else config.responseConfig.levelError
        config.logger.log(message, level)
    }
}

private suspend inline fun ByteReadChannel.tryReadText(charset: Charset): String? = try {
    readRemaining().readText(charset = charset)
} catch (cause: Throwable) {
    null
}

class AwesomeClientLoggingConfig {

    private var _logger: Logger? = null
    var logger: Logger
        get() = _logger ?: Logger.DEFAULT
        set(value) {
            _logger = value
        }

    var traceIdFieldName = "clientCallId"

    var requestConfig = RequestConfig(
        level = Level.TRACE,
        prefix = "Request >>> ",
        logBody = true,
        bodyDelimiter = " |> ",
    )

    var responseConfig = ResponseConfig(
        levelSuccess = Level.TRACE,
        levelError = Level.ERROR,
        prefix = "Response <<< ",
        logBodyOnError = true,
        bodyDelimiter = " <| ",
    )

    fun request(block: RequestConfig.() -> Unit) {
        block(requestConfig)
    }

    fun response(block: ResponseConfig.() -> Unit) {
        block(responseConfig)
    }

    class RequestConfig(
        var level: Level,
        var prefix: String,
        var logBody: Boolean,
        var bodyDelimiter: String,
    )

    class ResponseConfig(
        var levelSuccess: Level,
        var levelError: Level,
        var prefix: String,
        var logBodyOnError: Boolean,
        var bodyDelimiter: String,
    )

}

private suspend fun <T> withMdc(vararg infos: Pair<String, Any?>, func: suspend () -> T): T =
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
