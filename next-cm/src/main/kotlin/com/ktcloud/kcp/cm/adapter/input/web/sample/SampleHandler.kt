package com.ktcloud.kcp.cm.adapter.input.web.sample

import com.ktcloud.kcp.cm.adapter.input.web.sample.protocol.CreateSampleRequest
import com.ktcloud.kcp.cm.adapter.input.web.sample.protocol.SampleResponse
import com.ktcloud.kcp.cm.adapter.input.web.sample.protocol.SampleSearchRequest
import com.ktcloud.kcp.cm.adapter.input.web.sample.protocol.UpdateSampleRequest
import com.ktcloud.kcp.cm.application.port.input.sample.SampleUseCase
import com.ktcloud.kcp.cm.common.exception.InvalidPathParameterException
import com.ktcloud.kcp.cm.common.exception.RequiredRequestBodyException
import com.ktcloud.kcp.cm.common.extensions.*
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus
import jakarta.validation.Validator
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*

@Component
class SampleHandler(private val sampleUseCase: SampleUseCase, private val validator: Validator) {

    // ── GET /samples?name=...&minAge=...&maxAge=...&status=ACTIVE ─────────
    // QueryParam → model 바인딩 (bindQueryParams) + validation 예시
    // enum은 Spring WebDataBinder가 enum name 기반으로 자동 변환
    suspend fun searchSamples(request: ServerRequest): ServerResponse {
        val searchRequest = request.bindQueryParams<SampleSearchRequest>()
        validator.validateOrThrow(searchRequest)

        val result = sampleUseCase.searchSamples(searchRequest.toQuery())
        return ServerResponse.ok().bodyValueAndAwait(result.map(SampleResponse::from))
    }

    // ── GET /samples/status/{status} ─────────────────────────────────────
    // Path variable에서 enum 바인딩 예시: enumPathVariable<SampleStatus>("status")
    // 잘못된 enum 값이 들어오면 InvalidEnumPathParameterException 발생
    suspend fun searchSamplesByStatus(request: ServerRequest): ServerResponse {
        val status = request.enumPathVariable<SampleStatus>("status")

        val result = sampleUseCase.searchSamplesByStatus(status)
        return ServerResponse.ok().bodyValueAndAwait(result.map(SampleResponse::from))
    }

    // ── GET /samples/{id} ────────────────────────────────────────────────
    // PathVariable 예시 + 존재하지 않으면 404 에러 응답
    suspend fun getSample(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull()
            ?: throw InvalidPathParameterException("id")

        val result = sampleUseCase.getSample(id)
        return ServerResponse.ok().bodyValueAndAwait(SampleResponse.from(result))
    }

    // ── POST /samples ────────────────────────────────────────────────────
    // Body → model + validation 예시 (awaitBodyValidated)
    // Body에서 enum 바인딩: JSON { "name": "test", "age": 25, "status": "ACTIVE" }
    suspend fun createSample(request: ServerRequest): ServerResponse {
        val body = request.awaitBodyValidated<CreateSampleRequest>(validator)

        val result = sampleUseCase.createSample(body.toCommand())
        return ServerResponse.ok().bodyValueAndAwait(SampleResponse.from(result))
    }

    // ── PUT /samples/{id} ────────────────────────────────────────────────
    // PathVariable + Header + Body validation 종합 예시
    // Body에서 enum 바인딩: JSON { "name": "test", "age": 30, "status": "INACTIVE" }
    suspend fun updateSample(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull()
            ?: throw InvalidPathParameterException("id")
        val modifiedBy = request.headerOrThrow("X-Modified-By")
        val body = request.awaitBodyOrNull<UpdateSampleRequest>()
            ?.let(validator::validateOrThrow)
            ?: throw RequiredRequestBodyException()

        val command = body.toCommand(id, modifiedBy)
        val result = sampleUseCase.updateSample(command)
        return ServerResponse.ok().bodyValueAndAwait(SampleResponse.from(result))
    }

    // ── DELETE /samples/{id} ─────────────────────────────────────────────
    // PathVariable + 존재하지 않으면 404 에러 응답
    suspend fun deleteSample(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLongOrNull()
            ?: throw InvalidPathParameterException("id")

        sampleUseCase.deleteSample(id)
        return ServerResponse.noContent().buildAndAwait()
    }
}
