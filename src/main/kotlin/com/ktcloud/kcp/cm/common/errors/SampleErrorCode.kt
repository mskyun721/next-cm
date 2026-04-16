package com.ktcloud.kcp.cm.common.errors

enum class SampleErrorCode(override val code: String, override val label: String) : ErrorCode {
    SAMPLE_NOT_FOUND("ESMP001", "sample.SampleErrorCode.SAMPLE_NOT_FOUND"),
}
