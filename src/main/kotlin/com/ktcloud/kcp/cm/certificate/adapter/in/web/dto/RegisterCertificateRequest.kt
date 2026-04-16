package com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto

import com.ktcloud.kcp.cm.certificate.application.port.`in`.RegisterCertificateCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterCertificateRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 128)
    @field:Pattern(regexp = "^[A-Za-z0-9._\\-]+$")
    val alias: String,
    @field:NotBlank
    @field:Size(max = 32768)
    val pemContent: String,
) {
    fun toCommand(): RegisterCertificateCommand = RegisterCertificateCommand(alias = alias, pemContent = pemContent)
}
