package com.ktcloud.kcp.cm.application.port.input.sample

import com.ktcloud.kcp.cm.application.port.input.sample.model.CreateSampleCommand
import com.ktcloud.kcp.cm.application.port.input.sample.model.SampleSearchQuery
import com.ktcloud.kcp.cm.application.port.input.sample.model.UpdateSampleCommand
import com.ktcloud.kcp.cm.domain.sample.model.Sample
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus

interface SampleUseCase {
    suspend fun searchSamples(query: SampleSearchQuery): List<Sample>
    suspend fun searchSamplesByStatus(status: SampleStatus): List<Sample>
    suspend fun getSample(id: Long): Sample
    suspend fun createSample(command: CreateSampleCommand): Sample
    suspend fun updateSample(command: UpdateSampleCommand): Sample
    suspend fun deleteSample(id: Long)
}
