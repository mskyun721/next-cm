package com.ktcloud.kcp.cm.common

import com.ktcloud.kcp.cm.common.enums.DatePatternEnum
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import java.time.LocalDate
import java.time.LocalDateTime

private val kotlinModule = KotlinModule.Builder()
    .configure(KotlinFeature.NullToEmptyCollection, false)
    .configure(KotlinFeature.NullToEmptyMap, false)
    .configure(KotlinFeature.NullIsSameAsDefault, true)
    .configure(KotlinFeature.SingletonSupport, false)
    .configure(KotlinFeature.StrictNullChecks, false)
    .build()

private val dateTimeModule = SimpleModule().apply {
    addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(DatePatternEnum.DATETIME_DEFAULT.formatter))
    addDeserializer(LocalDate::class.java, LocalDateDeserializer(DatePatternEnum.DATE_DEFAULT.formatter))
    addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(DatePatternEnum.DATETIME_DEFAULT.formatter))
    addSerializer(LocalDate::class.java, LocalDateSerializer(DatePatternEnum.DATE_DEFAULT.formatter))
}

val objectMapper: JsonMapper = JsonMapper.builder()
    .addModule(kotlinModule)
    .addModule(dateTimeModule)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .build()
