package com.ktcloud.kcp.cm.adapter.input.web.sample.protocol

import com.ktcloud.kcp.cm.application.port.input.sample.model.SampleSearchQuery
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class SampleSearchRequest(
    val name: String? = null,
    @field:Min(0)
    val minAge: Int? = null,
    @field:Max(200)
    val maxAge: Int? = null,
    // QueryParam에서 enum 바인딩 예시: ?status=ACTIVE
    // Spring WebDataBinder가 enum name 기반으로 자동 변환
    val status: SampleStatus? = null,
) {
    fun toQuery() = SampleSearchQuery(name = name, minAge = minAge, maxAge = maxAge, status = status)
}
