package com.ktcloud.kcp.cm.application.port.input.sample.model

import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus

data class SampleSearchQuery(val name: String?, val minAge: Int?, val maxAge: Int?, val status: SampleStatus?)
