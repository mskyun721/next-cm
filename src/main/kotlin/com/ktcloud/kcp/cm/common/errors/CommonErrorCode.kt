package com.ktcloud.kcp.cm.common.errors

enum class CommonErrorCode(override val code: String, override val label: String) : ErrorCode {
    BAD_REQUEST("EKCP001", "common.CommonErrorCode.BAD_REQUEST"),
    UNAUTHORIZED("EKCP002", "common.CommonErrorCode.UNAUTHORIZED"),
    FORBIDDEN("EKCP003", "common.CommonErrorCode.FORBIDDEN"),
    NOT_FOUND("EKCP004", "common.CommonErrorCode.NOT_FOUND"),
    METHOD_NOT_ALLOWED("EKCP005", "common.CommonErrorCode.METHOD_NOT_ALLOWED"),
    NOT_ACCEPTABLE("EKCP006", "common.CommonErrorCode.NOT_ACCEPTABLE"),
    UNSUPPORTED_MEDIA_TYPE("EKCP007", "common.CommonErrorCode.UNSUPPORTED_MEDIA_TYPE"),
    INVALID_TOKEN("EKCP008", "common.CommonErrorCode.INVALID_TOKEN"),
    INVALID_HEADER_PARAMETER("EKCP009", "common.CommonErrorCode.INVALID_HEADER_PARAMETER"),
    VALIDATION_FAIL("EKCP010", "common.CommonErrorCode.VALIDATION_FAIL"),
    EMPTY_BODY("EKCP011", "common.CommonErrorCode.EMPTY_BODY"),
    INVALID_PARAMETER("EKCP012", "common.CommonErrorCode.INVALID_PARAMETER"),
    PAYLOAD_TOO_LARGE("EKCP013", "common.CommonErrorCode.PAYLOAD_TOO_LARGE"),
    JSON_PARSE_ERROR("EKCP014", "common.CommonErrorCode.JSON_PARSE_ERROR"),
    NOT_NULL("EKCP015", "common.CommonErrorCode.NOT_NULL"),
    INVALID_FORMAT("EKCP016", "common.CommonErrorCode.INVALID_FORMAT"),
    MISMATCH("EKCP017", "common.CommonErrorCode.MISMATCH"),
    CONFLICT("EKCP018", "common.CommonErrorCode.CONFLICT"),
    INTERNAL_SERVER_ERROR("EKCP999", "common.CommonErrorCode.INTERNAL_SERVER_ERROR"),
}
