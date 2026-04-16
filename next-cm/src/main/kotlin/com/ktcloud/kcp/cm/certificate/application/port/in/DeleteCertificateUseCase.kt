package com.ktcloud.kcp.cm.certificate.application.port.`in`

import java.util.UUID

interface DeleteCertificateUseCase {
    suspend fun delete(id: UUID)
}
