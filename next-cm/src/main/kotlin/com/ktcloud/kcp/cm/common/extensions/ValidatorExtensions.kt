package com.ktcloud.kcp.cm.common.extensions

import jakarta.validation.ConstraintViolationException
import jakarta.validation.Validator
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.awaitBody

fun <T : Any> Validator.validateOrThrow(target: T): T {
    val violations = if (target is Collection<*>) {
        target.asSequence().filterNotNull().flatMap { validate(it) }.toSet()
    } else {
        validate(target)
    }

    if (violations.isNotEmpty()) throw ConstraintViolationException(violations)
    return target
}

suspend inline fun <reified T : Any> ServerRequest.awaitBodyValidated(validator: Validator): T =
    validator.validateOrThrow(awaitBody<T>())
