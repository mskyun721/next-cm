package com.ktcloud.kcp.cm.common.config

import org.springframework.boot.http.client.HttpClientSettings
import org.springframework.boot.webclient.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Configuration(proxyBeanMethods = false)
class WebClientConfiguration {
    @Bean
    fun httpClientSettings(): HttpClientSettings =
        HttpClientSettings.defaults().withTimeouts(HTTP_CLIENT_TIMEOUT, HTTP_CLIENT_TIMEOUT)

    @Bean
    fun webClientCustomizer(): WebClientCustomizer = WebClientCustomizer { builder ->
        builder.codecs { configurer ->
            configurer.defaultCodecs().maxInMemorySize(-1)
        }
    }

    @Bean
    fun webClient(webClientBuilder: WebClient.Builder): WebClient = webClientBuilder.build()

    private companion object {
        val HTTP_CLIENT_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
