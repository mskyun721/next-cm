package com.ktcloud.kcp.cm.certificate.domain

import java.time.Instant
import java.util.UUID

data class Certificate(
    val id: UUID,
    val alias: String,
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val notBefore: Instant,
    val notAfter: Instant,
    val status: CertificateStatus,
    val pemContent: String,
    val fingerprint: String,
    val keyUsage: List<String>,
    val subjectAltNames: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun isExpired(at: Instant = Instant.now()): Boolean = at.isAfter(notAfter)

    fun isNotYetValid(at: Instant = Instant.now()): Boolean = at.isBefore(notBefore)

    fun daysUntilExpiry(from: Instant = Instant.now()): Long =
        java.time.Duration.between(from, notAfter).toDays()

    fun withStatus(newStatus: CertificateStatus, now: Instant = Instant.now()): Certificate =
        copy(status = newStatus, updatedAt = now)
}
