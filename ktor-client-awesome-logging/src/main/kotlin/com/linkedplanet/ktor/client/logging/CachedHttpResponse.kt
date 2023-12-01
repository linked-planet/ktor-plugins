package com.linkedplanet.ktor.client.logging

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import io.ktor.util.date.*
import io.ktor.utils.io.*
import kotlin.coroutines.CoroutineContext

class CachedHttpResponse(
    override val call: HttpClientCall,
    response: CachedHttpResponseData,
    override val coroutineContext: CoroutineContext
) : HttpResponse() {
    @InternalAPI
    override val content: ByteReadChannel = ByteReadChannel(response.body)
    override val headers: Headers = response.headers.headers.filterNot { it.name == "content-encoding" }.toHeaders()
    override val requestTime: GMTDate = GMTDate(response.headers.timestamp)
    override val responseTime: GMTDate = GMTDate()
    override val status: HttpStatusCode = HttpStatusCode.fromValue(response.headers.statusCode)
    override val version: HttpProtocolVersion = HttpProtocolVersion.HTTP_1_1
}

private fun List<CachedHttpHeaderValue>.toHeaders(): Headers = HeadersBuilder().apply {
    forEach { header -> append(header.name, header.value) }
}.build()

data class CachedHttpResponseData(val headers: CachedHttpHeaders, val body: String) {
    companion object {
        suspend fun create(response: HttpResponse): CachedHttpResponseData {
            val body = try {
                response.bodyAsText()
            } catch (e: Exception) {
                ""
            }
            return CachedHttpResponseData(CachedHttpHeaders(response), body)
        }
    }
}

data class CachedHttpHeaders(val statusCode: Int, val timestamp: Long, val headers: List<CachedHttpHeaderValue>) {
    constructor(response: HttpResponse) : this(
        response.status.value, response.responseTime.timestamp,
        response.headers.entries().flatMap { (name, values) ->
            values.map { CachedHttpHeaderValue(name, it) }
        }
    )
}

data class CachedHttpHeaderValue(val name: String, val value: String)
