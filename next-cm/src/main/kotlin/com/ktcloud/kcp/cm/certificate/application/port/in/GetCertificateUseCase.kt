package com.ktcloud.kcp.cm.certificate.application.port.`in`

import com.ktcloud.kcp.cm.certificate.domain.Certificate
import java.util.UUID

interface GetCertificateUseCase {
    suspend fun getCertificate(id: UUID): Certificate
    suspend fun listCertificates(): List<Certificate>
}
