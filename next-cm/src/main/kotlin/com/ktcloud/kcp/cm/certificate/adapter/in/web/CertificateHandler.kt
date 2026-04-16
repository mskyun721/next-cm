package com.ktcloud.kcp.cm.certificate.adapter.`in`.web

import com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto.CertificateResponse
import com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto.RegisterCertificateRequest
import com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto.ValidationResultResponse
import com.ktcloud.kcp.cm.certificate.application.port.`in`.DeleteCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.GetCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.RegisterCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.ValidateCertificateUseCase
import com.ktcloud.kcp.cm.common.exception.InvalidPathParameterException
import com.ktcloud.kcp.cm.common.extensions.awaitBodyValidated
import jakarta.validation.Validator
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.util.UUID

@Component
class CertificateHandler(
    private val registerUseCase: RegisterCertificateUseCase,
    private val getUseCase: GetCertificateUseCase,
    private val validateUseCase: ValidateCertificateUseCase,
    private val deleteUseCase: DeleteCertificateUseCase,
    private val validator: Validator,
) {

    suspend fun register(request: ServerRequest): ServerResponse {
        val body = request.awaitBodyValidated<RegisterCertificateRequest>(validator)
        val result = registerUseCase.register(body.toCommand())
        return ServerResponse.status(HttpStatus.CREATED).bodyValueAndAwait(CertificateResponse.from(result))
    }

    suspend fun list(@Suppress("UNUSED_PARAMETER") request: ServerRequest): ServerResponse {
        val result = getUseCase.listCertificates()
        return ServerResponse.ok().bodyValueAndAwait(result.map { CertificateResponse.from(it) })
    }

    suspend fun get(request: ServerRequest): ServerResponse {
        val id = parseId(request)
        val result = getUseCase.getCertificate(id)
        return ServerResponse.ok().bodyValueAndAwait(CertificateResponse.from(result))
    }

    suspend fun validate(request: ServerRequest): ServerResponse {
        val id = parseId(request)
        val result = validateUseCase.validate(id)
        return ServerResponse.ok().bodyValueAndAwait(ValidationResultResponse.from(result))
    }

    suspend fun delete(request: ServerRequest): ServerResponse {
        val id = parseId(request)
        deleteUseCase.delete(id)
        return ServerResponse.noContent().buildAndAwait()
    }

    private fun parseId(request: ServerRequest): UUID = try {
        UUID.fromString(request.pathVariable("id"))
    } catch (_: IllegalArgumentException) {
        throw InvalidPathParameterException("id")
    }
}
