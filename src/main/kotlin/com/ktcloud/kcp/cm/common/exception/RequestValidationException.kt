package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.ApiFieldError
import com.ktcloud.kcp.cm.common.errors.ErrorCode
import org.springframework.http.HttpStatus

open class RequestValidationException(
    status: HttpStatus = HttpStatus.BAD_REQUEST,
    errorCode: ErrorCode,
    val fieldErrors: List<ApiFieldError>,
    cause: Throwable? = null,
) : DefaultException(status = status, errorCode = errorCode, cause = cause)
