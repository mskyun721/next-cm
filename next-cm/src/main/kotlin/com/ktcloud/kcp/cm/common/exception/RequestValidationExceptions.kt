package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.*
import org.springframework.http.HttpStatus

open class SingleFieldRequestValidationException(
    status: HttpStatus = HttpStatus.BAD_REQUEST,
    errorCode: ErrorCode,
    source: ErrorSource,
    field: String,
    message: String,
    cause: Throwable? = null,
) : RequestValidationException(
    status = status,
    errorCode = errorCode,
    fieldErrors = listOf(
        ApiFieldError(
            source = source.wireName,
            field = field,
            message = message,
        ),
    ),
    cause = cause,
)

open class InvalidRequestFieldException(
    source: ErrorSource,
    field: String,
    errorCode: ErrorCode,
    messageArguments: Array<Any> = arrayOf(field),
    cause: Throwable? = null,
) : SingleFieldRequestValidationException(
    errorCode = errorCode,
    source = source,
    field = field,
    message = errorCode.getMessage(messageArguments),
    cause = cause,
)

class RequiredHeaderException(field: String) :
    InvalidRequestFieldException(
        source = ErrorSource.HEADER,
        field = field,
        errorCode = CommonErrorCode.INVALID_HEADER_PARAMETER,
    )

class InvalidHeaderValueException(field: String) :
    InvalidRequestFieldException(
        source = ErrorSource.HEADER,
        field = field,
        errorCode = CommonErrorCode.INVALID_HEADER_PARAMETER,
    )

class RequiredQueryParameterException(field: String) :
    InvalidRequestFieldException(
        source = ErrorSource.QUERY,
        field = field,
        errorCode = CommonErrorCode.INVALID_PARAMETER,
    )

open class InvalidPathParameterException(field: String) :
    InvalidRequestFieldException(
        source = ErrorSource.PATH,
        field = field,
        errorCode = CommonErrorCode.INVALID_PARAMETER,
    )

class InvalidEnumPathParameterException(field: String) : InvalidPathParameterException(field)

class RequiredRequestBodyException(field: String = ErrorFieldNames.BODY) :
    SingleFieldRequestValidationException(
        errorCode = CommonErrorCode.EMPTY_BODY,
        source = ErrorSource.BODY,
        field = field,
        message = CommonErrorCode.EMPTY_BODY.getMessage(),
    )
