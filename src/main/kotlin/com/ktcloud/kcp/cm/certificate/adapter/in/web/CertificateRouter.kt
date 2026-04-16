package com.ktcloud.kcp.cm.certificate.adapter.`in`.web

import com.ktcloud.kcp.cm.common.constant.CommonConstant.API_VERSION_V1
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates.version
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.coRouter

@Configuration
class CertificateRouter(private val handler: CertificateHandler) {
    @Bean
    fun certificateRoutes(): RouterFunction<ServerResponse> = coRouter {
        (accept(MediaType.APPLICATION_JSON) and version(API_VERSION_V1) and "/api/{version}").nest {
            POST("/certificates", handler::register)
            GET("/certificates", handler::list)
            GET("/certificates/{id}", handler::get)
            POST("/certificates/{id}/validate", handler::validate)
            DELETE("/certificates/{id}", handler::delete)
        }
    }
}
