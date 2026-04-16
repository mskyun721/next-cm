package com.ktcloud.kcp.cm.adapter.input.web.sample.protocol

import com.ktcloud.kcp.cm.application.port.input.sample.model.UpdateSampleCommand
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateSampleRequest(
    @field:NotBlank
    val name: String,
    @field:Min(0)
    @field:Max(200)
    val age: Int,
    // Body에서 enum 바인딩 예시: JSON { "status": "INACTIVE" }
    @field:NotNull
    val status: SampleStatus,
) {
    fun toCommand(id: Long, modifiedBy: String) = UpdateSampleCommand(
        id = id,
        name = name,
        age = age,
        status = status,
        modifiedBy = modifiedBy,
    )
}
