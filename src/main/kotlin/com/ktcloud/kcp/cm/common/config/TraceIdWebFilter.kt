package com.ktcloud.kcp.cm.common.config

import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.*

@Component
class TraceIdWebFilter : WebFilter {
    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val traceId = resolveTraceId(exchange.request)
        exchange.attributes[TRACE_ID_ATTRIBUTE] = traceId

        exchange.response.beforeCommit {
            exchange.response.headers.set(TRACE_ID_HEADER, traceId)
            Mono.empty()
        }

        return chain.filter(exchange)
            .contextWrite { context ->
                context.put(Slf4jThreadLocalAccessor.KEY, mapOf(MDC_TRACE_ID_KEY to traceId))
            }
    }

    private fun resolveTraceId(request: ServerHttpRequest): String = request.headers.getFirst(TRACE_ID_HEADER)
        ?.takeIf { it.isNotBlank() }
        ?: extractTraceIdFromTraceParent(request.headers.getFirst(TRACE_PARENT_HEADER))
        ?: request.headers.getFirst(B3_TRACE_ID_HEADER)
            ?.takeIf { it.isNotBlank() }
            ?.lowercase()
        ?: generateTraceId()

    private fun extractTraceIdFromTraceParent(traceParent: String?): String? {
        if (traceParent.isNullOrBlank()) {
            return null
        }

        val segments = traceParent.split("-")
        if (segments.size != TRACE_PARENT_SEGMENT_COUNT) {
            return null
        }

        val traceId = segments[1].lowercase()
        return traceId.takeIf { TRACE_PARENT_TRACE_ID_REGEX.matches(it) }
    }

    private fun generateTraceId(): String = UUID.randomUUID().toString().replace("-", "")

    companion object {
        const val TRACE_ID_ATTRIBUTE = "traceId"
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_PARENT_HEADER = "traceparent"
        const val B3_TRACE_ID_HEADER = "X-B3-TraceId"
        const val MDC_TRACE_ID_KEY = "traceId"
        private const val TRACE_PARENT_SEGMENT_COUNT = 4
        val TRACE_PARENT_TRACE_ID_REGEX = Regex("^[0-9a-f]{32}$")
    }
}
