package com.ktcloud.kcp.cm.common.extensions

import com.ktcloud.kcp.cm.common.enums.DisplayEnum
import com.ktcloud.kcp.cm.common.enums.GenericEnum
import com.ktcloud.kcp.cm.common.exception.InvalidEnumValueException
import kotlin.reflect.KClass

inline fun <reified E> byLabel(label: String): E? where E : Enum<E>, E : DisplayEnum = enumValues<E>().firstOrNull {
    it.label == label
}

// GenericEnum.value 기반 enum 조회 (DB → enum 변환 용도)
inline fun <reified E> byValue(value: String): E? where E : Enum<E>, E : GenericEnum = enumValues<E>().firstOrNull {
    it.value == value
}

// GenericEnum.value 기반 enum 조회 — 실패 시 InvalidEnumValueException 발생
// 사용법: requireByValue<SampleStatus>("active")
inline fun <reified E> requireByValue(value: String): E where E : Enum<E>, E : GenericEnum = byValue<E>(value)
    ?: throw InvalidEnumValueException(enumClass = E::class, value = value)

inline fun <reified T> KClass<T>.toDocument(): String where T : Enum<T>, T : DisplayEnum = enumValues<T>().filter {
    it.displayable
}
    .sortedBy { it.priority }
    .joinToString(separator = ",", prefix = "[", postfix = "]") {
        "${it.name}: ${it.label}"
    }
