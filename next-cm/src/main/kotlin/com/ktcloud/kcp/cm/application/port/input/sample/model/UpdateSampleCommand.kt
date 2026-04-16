package com.ktcloud.kcp.cm.application.port.input.sample.model

import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus

data class UpdateSampleCommand(
    val id: Long,
    val name: String,
    val age: Int,
    val status: SampleStatus,
    val modifiedBy: String,
)
