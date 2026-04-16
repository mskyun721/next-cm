package com.ktcloud.kcp.cm.common.config

import com.ktcloud.kcp.cm.common.constant.CommonConstant.DEFAULT_LOCALE
import com.ktcloud.kcp.cm.common.errors.*
import com.ktcloud.kcp.cm.common.exception.DefaultException
import com.ktcloud.kcp.cm.common.exception.RequestValidationException
import com.ktcloud.kcp.cm.common.utils.MessageConverter
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.Range
import org.springframework.boot.json.JsonParseException
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.webflux.error.DefaultErrorAttributes
import org.springframework.core.io.buffer.DataBufferLimitException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.InvalidMediaTypeException
import org.springframework.validation.MessageCodesResolver
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebInputException
import tools.jackson.databind.exc.InvalidFormatException
import tools.jackson.databind.exc.MismatchedInputException
import java.util.*

class GlobalErrorAttributes(private val messageResolver: MessageCodesResolver) : DefaultErrorAttributes() {
    override fun getErrorAttributes(request: ServerRequest, options: ErrorAttributeOptions): Map<String, Any> {
        val error = getError(request)
        val locale = request.exchange().localeContext.locale ?: DEFAULT_LOCALE
        val errorResult = getErrorResult(error, request, locale)

        return linkedMapOf<String, Any>(
            "status" to errorResult.status,
            "code" to errorResult.code,
            "message" to errorResult.message,
            "path" to errorResult.path,
            "traceId" to (errorResult.traceId ?: ""),
        ).apply {
            errorResult.errors
                ?.takeIf { it.isNotEmpty() }
                ?.let { put("errors", it) }
        }
    }

    // 순서 중요: DefaultException 분기에서 RequestValidationException 도 함께 처리
    private fun getErrorResult(error: Throwable, request: ServerRequest, locale: Locale): ApiErrorResponse =
        when (error) {
            is DefaultException -> handleDefaultException(error, request, locale)
            is WebExchangeBindException -> handleWebExchangeBindException(error, request, locale)
            is ConstraintViolationException -> handleConstraintViolationException(error, request, locale)
            is ServerWebInputException -> handleServerWebInputException(error, request, locale)
            is InvalidMediaTypeException -> handleInvalidMediaTypeException(request, locale)
            is DataBufferLimitException -> handleDataBufferLimitException(request, locale)
            is ResponseStatusException -> handleResponseStatusException(error, request, locale)
            // WHY: DB UNIQUE constraint violations (e.g. duplicate fingerprint) must surface as 409,
            // not 500. This is the final safety net when the pre-check race is lost.
            is DataIntegrityViolationException -> handleDataIntegrityViolationException(request, locale)
            else -> handleUnknownException(request, locale)
        }

    private fun handleDefaultException(
        ex: DefaultException,
        request: ServerRequest,
        locale: Locale,
    ): ApiErrorResponse = errorResponse(
        status = ex.status,
        errorCode = ex.errorCode,
        request = request,
        locale = locale,
        message = ex.errorCode.getMessage(ex.messageArguments, locale),
        errors = (ex as? RequestValidationException)?.fieldErrors,
    )

    // Spring Web Validation 관련 처리
    private fun handleWebExchangeBindException(
        ex: WebExchangeBindException,
        request: ServerRequest,
        locale: Locale,
    ): ApiErrorResponse {
        val validationErrors = ex.bindingResult.fieldErrors.map { fieldError ->
            val message = fieldError.defaultMessage
                ?: fieldError.codes
                    ?.firstNotNullOfOrNull { code ->
                        MessageConverter.getMessageOrNull(code, fieldError.arguments, locale)
                    }
                ?: ErrorMessages.INVALID_VALUE

            ApiFieldError(
                source = ErrorSource.BODY.wireName,
                field = fieldError.field,
                message = message,
            )
        }.sortedBy { it.field }

        return errorResponse(
            status = HttpStatus.BAD_REQUEST,
            errorCode = CommonErrorCode.VALIDATION_FAIL,
            request = request,
            locale = locale,
            errors = validationErrors,
        )
    }

    private fun handleServerWebInputException(
        ex: ServerWebInputException,
        request: ServerRequest,
        locale: Locale,
    ): ApiErrorResponse {
        val httpStatus = HttpStatus.BAD_REQUEST
        val errorCode: ErrorCode
        val errors: List<ApiFieldError>?

        when (val rootCause = ex.cause) {
            is InvalidFormatException -> {
                errorCode = CommonErrorCode.INVALID_FORMAT
                errors = listOf(
                    ApiFieldError(
                        source = ErrorSource.BODY.wireName,
                        field = rootCause.path.joinToString(".") { reference ->
                            reference.propertyName ?: if (reference.index >= 0) "[${reference.index}]" else ""
                        }.ifBlank { ErrorFieldNames.BODY },
                        message = rootCause.originalMessage
                            ?: CommonErrorCode.INVALID_FORMAT.getMessage(locale = locale),
                    ),
                )
            }

            is MismatchedInputException -> {
                errorCode = CommonErrorCode.MISMATCH
                errors = listOf(
                    ApiFieldError(
                        source = ErrorSource.BODY.wireName,
                        field = rootCause.path.joinToString(".") { reference ->
                            reference.propertyName ?: if (reference.index >= 0) "[${reference.index}]" else ""
                        }.ifBlank { ErrorFieldNames.BODY },
                        message = rootCause.originalMessage ?: CommonErrorCode.MISMATCH.getMessage(locale = locale),
                    ),
                )
            }

            is JsonParseException -> {
                errorCode = CommonErrorCode.JSON_PARSE_ERROR
                errors = listOf(
                    ApiFieldError(
                        source = ErrorSource.BODY.wireName,
                        field = ErrorFieldNames.BODY,
                        message = CommonErrorCode.JSON_PARSE_ERROR.getMessage(locale = locale),
                    ),
                )
            }

            else -> {
                errorCode = CommonErrorCode.BAD_REQUEST
                errors = null
            }
        }

        return errorResponse(
            status = httpStatus,
            errorCode = errorCode,
            request = request,
            locale = locale,
            errors = errors,
        )
    }

    private fun handleResponseStatusException(
        ex: ResponseStatusException,
        request: ServerRequest,
        locale: Locale,
    ): ApiErrorResponse {
        val status = HttpStatus.resolve(ex.statusCode.value()) ?: HttpStatus.INTERNAL_SERVER_ERROR
        val errorCode = when (status) {
            HttpStatus.BAD_REQUEST -> CommonErrorCode.BAD_REQUEST
            HttpStatus.UNAUTHORIZED -> CommonErrorCode.UNAUTHORIZED
            HttpStatus.FORBIDDEN -> CommonErrorCode.FORBIDDEN
            HttpStatus.NOT_FOUND -> CommonErrorCode.NOT_FOUND
            HttpStatus.METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED
            HttpStatus.NOT_ACCEPTABLE -> CommonErrorCode.NOT_ACCEPTABLE
            HttpStatus.UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE
            else -> CommonErrorCode.INTERNAL_SERVER_ERROR
        }

        return errorResponse(
            status = status,
            errorCode = errorCode,
            request = request,
            locale = locale,
        )
    }

    // Bean Validation 실패
    private fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: ServerRequest,
        locale: Locale,
    ): ApiErrorResponse {
        val errors = ex.constraintViolations.mapNotNull { violation ->
            val property = extractLeafFieldName(violation.propertyPath.toString())
            val constraint = violation.constraintDescriptor.annotation.annotationClass.simpleName ?: "Unknown"
            val objectName = violation.rootBeanClass?.simpleName?.lowercase() ?: "object"
            val args = extractConstraintArguments(violation.constraintDescriptor.annotation)
            val messageCodes = messageResolver.resolveMessageCodes(
                constraint,
                objectName,
                property,
                violation.leafBean.javaClass,
            )
            val message = violation.message
                ?: messageCodes.firstNotNullOfOrNull { code ->
                    MessageConverter.getMessageOrNull(code, args, locale)
                }
                ?: property

            ApiFieldError(
                source = ErrorSource.BODY.wireName,
                field = property,
                message = message,
            )
        }.sortedBy { it.field }

        return errorResponse(
            status = HttpStatus.BAD_REQUEST,
            errorCode = CommonErrorCode.VALIDATION_FAIL,
            request = request,
            locale = locale,
            errors = errors,
        )
    }

    private fun handleInvalidMediaTypeException(request: ServerRequest, locale: Locale): ApiErrorResponse =
        errorResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            errorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
            request = request,
            locale = locale,
        )

    private fun handleDataBufferLimitException(request: ServerRequest, locale: Locale): ApiErrorResponse =
        errorResponse(
            status = HttpStatus.PAYLOAD_TOO_LARGE,
            errorCode = CommonErrorCode.PAYLOAD_TOO_LARGE,
            request = request,
            locale = locale,
        )

    private fun handleDataIntegrityViolationException(request: ServerRequest, locale: Locale): ApiErrorResponse =
        errorResponse(
            status = HttpStatus.CONFLICT,
            errorCode = CommonErrorCode.CONFLICT,
            request = request,
            locale = locale,
        )

    private fun handleUnknownException(request: ServerRequest, locale: Locale): ApiErrorResponse = errorResponse(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR,
        request = request,
        locale = locale,
    )
}

private fun errorResponse(
    status: HttpStatus,
    errorCode: ErrorCode,
    request: ServerRequest,
    locale: Locale,
    message: String = errorCode.getMessage(locale = locale),
    errors: List<ApiFieldError>? = null,
): ApiErrorResponse = ApiErrorResponse(
    status = status.value(),
    code = errorCode.code,
    message = message,
    path = request.path(),
    traceId = request.exchange().getAttribute<String>(TraceIdWebFilter.TRACE_ID_ATTRIBUTE)
        ?: request.exchange().request.id,
    errors = errors,
)

private fun extractConstraintArguments(annotation: Annotation): Array<Any> = when (annotation) {
    is Range -> arrayOf(annotation.min, annotation.max)
    is Size -> arrayOf(annotation.min, annotation.max)
    is Min -> arrayOf(annotation.value)
    is Max -> arrayOf(annotation.value)
    is DecimalMin -> arrayOf(annotation.value)
    is DecimalMax -> arrayOf(annotation.value)
    is Pattern -> arrayOf(annotation.regexp)
    else -> emptyArray()
}

private fun extractLeafFieldName(propertyPath: String): String = propertyPath.substringAfterLast('.').ifBlank {
    propertyPath
}
