package com.ktcloud.kcp.cm.common.config

import io.micrometer.context.ContextRegistry
import io.micrometer.context.integration.Slf4jThreadLocalAccessor
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Hooks

@Configuration(proxyBeanMethods = false)
class TraceLoggingConfiguration {
    @PostConstruct
    fun configureContextPropagation() {
        val registry = ContextRegistry.getInstance()
        registry.removeThreadLocalAccessor(Slf4jThreadLocalAccessor.KEY)
        registry.registerThreadLocalAccessor(Slf4jThreadLocalAccessor(TraceIdWebFilter.MDC_TRACE_ID_KEY))
        Hooks.enableAutomaticContextPropagation()
    }
}
