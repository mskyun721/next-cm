package com.ktcloud.kcp.cm.certificate.application.port.out

import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.CertificateStatus
import java.util.UUID

interface CertificateRepository {
    fun save(certificate: Certificate): Certificate
    fun findById(id: UUID): Certificate?
    fun findByFingerprint(fingerprint: String): Certificate?
    fun findAll(): List<Certificate>
    fun updateStatus(id: UUID, status: CertificateStatus): Boolean
    fun delete(id: UUID): Boolean
}
