package com.ktcloud.kcp.cm.common.extensions

import com.ktcloud.kcp.cm.common.exception.InvalidEnumPathParameterException
import com.ktcloud.kcp.cm.common.exception.QueryParameterBindingException
import com.ktcloud.kcp.cm.common.exception.RequiredHeaderException
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.validation.BindException
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.reactive.function.server.ServerRequest
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

// ── Path Variable ────────────────────────────────────────────────────────
// Enum path variable 바인딩 예시: request.enumPathVariable<SampleStatus>("status")
inline fun <reified E : Enum<E>> ServerRequest.enumPathVariable(name: String): E = try {
    enumValueOf<E>(pathVariable(name).uppercase())
} catch (_: IllegalArgumentException) {
    throw InvalidEnumPathParameterException(name)
}

// ── Header ───────────────────────────────────────────────────────────────

suspend inline fun <reified T : Any> ServerRequest.bindQueryParams(): T = bindQueryParams(T::class)

suspend inline fun <reified T : Any> ServerRequest.bindQueryParams(
    noinline dataBinderCustomizer: (WebDataBinder) -> Unit,
): T = bindQueryParams(T::class, dataBinderCustomizer)

fun ServerRequest.headerOrThrow(name: String): String =
    this.headers().firstHeader(name) ?: throw RequiredHeaderException(name)

suspend fun <T : Any> ServerRequest.bindQueryParams(clazz: KClass<T>): T = bindQueryParams(clazz) { }

suspend fun <T : Any> ServerRequest.bindQueryParams(
    clazz: KClass<T>,
    dataBinderCustomizer: (WebDataBinder) -> Unit,
): T = try {
    bind(clazz.java, dataBinderCustomizer).awaitSingleOrNull()
        ?: throw QueryParameterBindingException(resolveQueryField(clazz))
} catch (exception: BindException) {
    throw QueryParameterBindingException(resolveBindField(exception), exception)
} catch (exception: IllegalStateException) {
    throw QueryParameterBindingException(resolveQueryField(clazz), exception)
}

private fun resolveBindField(exception: BindException): String =
    exception.bindingResult.fieldErrors.firstOrNull()?.field
        ?: exception.bindingResult.globalErrors.firstOrNull()?.objectName
        ?: "query"

private fun <T : Any> ServerRequest.resolveQueryField(clazz: KClass<T>): String {
    val queryFields = queryParams().keys
    val constructorParams = clazz.primaryConstructor?.parameters.orEmpty()
    val requiredFields = constructorParams
        .filter { parameter -> !parameter.isOptional && !parameter.type.isMarkedNullable }
        .mapNotNull(KParameter::name)

    // 1. 누락된 required 필드가 있으면 해당 필드 반환
    val missingField = requiredFields.firstOrNull { it !in queryFields }
    if (missingField != null) return missingField

    // 2. 모든 required 필드가 전달된 경우 (타입 변환 실패 등)
    //    전달된 query parameter 중 constructor에 매칭되는 것을 반환
    val allParamNames = constructorParams.mapNotNull(KParameter::name)
    return queryFields.firstOrNull { it in allParamNames }
        ?: allParamNames.firstOrNull()
        ?: clazz.simpleName
        ?: "query"
}
