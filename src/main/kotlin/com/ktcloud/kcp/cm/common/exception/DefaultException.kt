package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.ErrorCode
import org.springframework.http.HttpStatus

abstract class DefaultException(
    val status: HttpStatus,
    val errorCode: ErrorCode,
    val messageArguments: Array<Any> = emptyArray(),
    cause: Throwable? = null,
) : RuntimeException(errorCode.getMessage(messageArguments), cause) {
    override val message: String
        get() = super.message ?: ""
}
