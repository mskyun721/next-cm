package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.CertificateErrorCode
import org.springframework.http.HttpStatus
import java.util.UUID

class CertificateNotFoundException(id: UUID) :
    DefaultException(HttpStatus.NOT_FOUND, CertificateErrorCode.CERTIFICATE_NOT_FOUND, arrayOf(id.toString()))

class CertificateAlreadyRegisteredException(fingerprint: String) :
    DefaultException(
        HttpStatus.CONFLICT,
        CertificateErrorCode.CERTIFICATE_ALREADY_REGISTERED,
        arrayOf(maskFingerprint(fingerprint)),
    )

class InvalidPemFormatException(detail: String, cause: Throwable? = null) :
    DefaultException(
        HttpStatus.BAD_REQUEST,
        CertificateErrorCode.INVALID_PEM_FORMAT,
        arrayOf(detail),
        cause,
    )

// WHY: full fingerprint shouldn't leak into error messages/logs (PII/operational hygiene).
private fun maskFingerprint(fingerprint: String): String =
    if (fingerprint.length <= FINGERPRINT_PREVIEW_LENGTH) fingerprint
    else fingerprint.take(FINGERPRINT_PREVIEW_LENGTH) + "..."

private const val FINGERPRINT_PREVIEW_LENGTH = 12