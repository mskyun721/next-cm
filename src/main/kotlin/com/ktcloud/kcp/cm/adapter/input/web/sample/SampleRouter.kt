package com.ktcloud.kcp.cm.adapter.input.web.sample

import com.ktcloud.kcp.cm.common.constant.CommonConstant.API_VERSION_V1
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.version
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class SampleRouter(private val handler: SampleHandler) {
    @Bean
    fun sampleRoutes(): RouterFunction<ServerResponse> = coRouter {
        (accept(MediaType.APPLICATION_JSON) and version(API_VERSION_V1) and "/sample/{version}").nest {
            GET("samples", handler::searchSamples)
            GET("samples/status/{status}", handler::searchSamplesByStatus) // Path variable enum 예시
            GET("samples/{id}", handler::getSample)
            POST("samples", handler::createSample)
            PUT("samples/{id}", handler::updateSample)
            DELETE("samples/{id}", handler::deleteSample)
        }
    }
}
