package com.ktcloud.kcp.cm.common.errors

data class ApiErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val traceId: String?,
    val errors: List<ApiFieldError>? = null,
)

data class ApiFieldError(val source: String, val field: String, val message: String)

enum class ErrorSource(val wireName: String) {
    BODY("BODY"),
    QUERY("QUERY"),
    PATH("PATH"),
    HEADER("HEADER"),
}
