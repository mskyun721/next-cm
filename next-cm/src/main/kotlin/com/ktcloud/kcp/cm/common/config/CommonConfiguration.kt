package com.ktcloud.kcp.cm.common.config

import com.ktcloud.kcp.cm.common.objectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.json.JsonMapper

@Configuration
class CommonConfiguration {
    @Bean
    @Primary
    fun applicationObjectMapper(): JsonMapper = objectMapper
}
