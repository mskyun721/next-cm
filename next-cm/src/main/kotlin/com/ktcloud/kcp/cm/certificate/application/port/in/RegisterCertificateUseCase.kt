package com.ktcloud.kcp.cm.certificate.application.port.`in`

import com.ktcloud.kcp.cm.certificate.domain.Certificate

interface RegisterCertificateUseCase {
    suspend fun register(command: RegisterCertificateCommand): Certificate
}

data class RegisterCertificateCommand(
    val alias: String,
    val pemContent: String,
)
