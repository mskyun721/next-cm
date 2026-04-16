package com.ktcloud.kcp.cm.certificate.application.port.out

import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.ValidationResult

interface CertificateValidationPort {
    /**
     * Parse a PEM encoded X.509 certificate into a domain [Certificate].
     * The produced [Certificate] has id/alias/timestamps filled by the caller.
     */
    fun parse(pemContent: String): ParsedCertificate

    /**
     * Validate the given certificate against trust store, expiry and key usage policies.
     */
    fun validate(certificate: Certificate): ValidationResult
}

data class ParsedCertificate(
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val notBefore: java.time.Instant,
    val notAfter: java.time.Instant,
    val fingerprint: String,
    val keyUsage: List<String>,
    val subjectAltNames: List<String>,
)
