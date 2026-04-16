package com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto

import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.CertificateStatus
import java.time.Instant
import java.util.UUID

data class CertificateResponse(
    val id: UUID,
    val alias: String,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val notBefore: Instant,
    val notAfter: Instant,
    val status: CertificateStatus,
    val fingerprint: String,
    val keyUsage: List<String>,
    val subjectAltNames: List<String>,
    val daysUntilExpiry: Long,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(certificate: Certificate, now: Instant = Instant.now()): CertificateResponse = CertificateResponse(
            id = certificate.id,
            alias = certificate.alias,
            subject = certificate.subject,
            issuer = certificate.issuer,
            serialNumber = certificate.serialNumber,
            notBefore = certificate.notBefore,
            notAfter = certificate.notAfter,
            status = certificate.status,
            fingerprint = certificate.fingerprint,
            keyUsage = certificate.keyUsage,
            subjectAltNames = certificate.subjectAltNames,
            daysUntilExpiry = certificate.daysUntilExpiry(now),
            createdAt = certificate.createdAt,
            updatedAt = certificate.updatedAt,
        )
    }
}
