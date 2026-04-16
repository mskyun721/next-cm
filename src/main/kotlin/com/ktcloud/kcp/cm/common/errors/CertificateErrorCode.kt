package com.ktcloud.kcp.cm.common.errors

enum class CertificateErrorCode(override val code: String, override val label: String) : ErrorCode {
    CERTIFICATE_NOT_FOUND("ECRT001", "certificate.CertificateErrorCode.CERTIFICATE_NOT_FOUND"),
    CERTIFICATE_ALREADY_REGISTERED("ECRT002", "certificate.CertificateErrorCode.CERTIFICATE_ALREADY_REGISTERED"),
    INVALID_PEM_FORMAT("ECRT003", "certificate.CertificateErrorCode.INVALID_PEM_FORMAT"),
}