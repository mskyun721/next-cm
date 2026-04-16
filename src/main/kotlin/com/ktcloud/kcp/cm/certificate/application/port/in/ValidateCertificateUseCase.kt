package com.ktcloud.kcp.cm.certificate.application.port.`in`

import com.ktcloud.kcp.cm.certificate.domain.ValidationResult
import java.util.UUID

interface ValidateCertificateUseCase {
    suspend fun validate(id: UUID): ValidationResult
}
