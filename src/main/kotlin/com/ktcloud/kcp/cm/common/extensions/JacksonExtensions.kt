package com.ktcloud.kcp.cm.common.extensions

import com.ktcloud.kcp.cm.common.objectMapper
import tools.jackson.module.kotlin.jacksonTypeRef

fun Any?.toJsonOrNull(): String? = try {
    this?.let { objectMapper.writeValueAsString(it) }
} catch (ignored: Exception) {
    null
}

fun Any?.toJson(default: String): String = try {
    this?.let { objectMapper.writeValueAsString(this) } ?: default
} catch (ignored: Exception) {
    default
}

fun Any.toJson(): String = objectMapper.writeValueAsString(this)

inline fun <reified T> String?.toModelOrNull(): T? = try {
    this?.let { objectMapper.readValue(this, jacksonTypeRef<T>()) }
} catch (ignored: Exception) {
    null
}

inline fun <reified T> String?.toModel(default: T): T = try {
    this?.let { objectMapper.readValue(this, jacksonTypeRef<T>()) } ?: default
} catch (ignored: Exception) {
    default
}

inline fun <reified T> String.toModel(): T = objectMapper.readValue(this, jacksonTypeRef<T>())
