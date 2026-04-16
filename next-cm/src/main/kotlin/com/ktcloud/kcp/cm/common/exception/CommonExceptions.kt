package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.CommonErrorCode
import com.ktcloud.kcp.cm.common.errors.ErrorSource
import org.springframework.http.HttpStatus
import kotlin.reflect.KClass

class UnauthorizedException : DefaultException(HttpStatus.UNAUTHORIZED, CommonErrorCode.UNAUTHORIZED)

class InvalidTokenException : DefaultException(HttpStatus.UNAUTHORIZED, CommonErrorCode.INVALID_TOKEN)

class PermissionDeniedException : DefaultException(HttpStatus.FORBIDDEN, CommonErrorCode.FORBIDDEN)

class QueryParameterBindingException(field: String, cause: Throwable? = null) :
    InvalidRequestFieldException(
        source = ErrorSource.QUERY,
        field = field,
        errorCode = CommonErrorCode.INVALID_PARAMETER,
        cause = cause,
    )

// GenericEnum.value 변환 실패 시 사용 (DB value → enum 변환 등)
// 서비스/영속성 계층에서 발생하는 데이터 무결성 문제이므로 500 INTERNAL_SERVER_ERROR
// Request 계층의 enum 변환은 전용 예외 사용:
//   - Path:  InvalidEnumPathParameterException (400)
//   - Query: QueryParameterBindingException (400)
//   - Body:  Jackson ServerWebInputException (400)
class InvalidEnumValueException(enumClass: KClass<*>, value: String) :
    DefaultException(
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR,
        messageArguments = arrayOf("${enumClass.simpleName}($value)"),
    )
