package com.ktcloud.kcp.cm.application.service

import com.ktcloud.kcp.cm.application.port.input.sample.SampleUseCase
import com.ktcloud.kcp.cm.application.port.input.sample.model.CreateSampleCommand
import com.ktcloud.kcp.cm.application.port.input.sample.model.SampleSearchQuery
import com.ktcloud.kcp.cm.application.port.input.sample.model.UpdateSampleCommand
import com.ktcloud.kcp.cm.application.port.output.sample.SamplePort
import com.ktcloud.kcp.cm.application.port.output.transaction.TransactionalPort
import com.ktcloud.kcp.cm.common.exception.SampleNotFoundException
import com.ktcloud.kcp.cm.domain.sample.model.Sample
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SampleService(private val transactionalPort: TransactionalPort, private val samplePort: SamplePort) :
    SampleUseCase {

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun searchSamples(query: SampleSearchQuery): List<Sample> = transactionalPort.executeReadOnly {
        log.debug("searchSamples() - query={}", query)
        samplePort.findByFilter(query.name, query.minAge, query.maxAge, query.status)
    }

    override suspend fun searchSamplesByStatus(status: SampleStatus): List<Sample> = transactionalPort.executeReadOnly {
        log.debug("searchSamplesByStatus() - status={}", status)
        samplePort.findByStatus(status)
    }

    override suspend fun getSample(id: Long): Sample = transactionalPort.executeReadOnly {
        samplePort.findById(id)
    } ?: throw SampleNotFoundException(id)

    override suspend fun createSample(command: CreateSampleCommand): Sample = transactionalPort.execute {
        log.debug("createSample() - command={}", command)
        samplePort.insert(command.name, command.age, command.status)
    }

    override suspend fun updateSample(command: UpdateSampleCommand): Sample = transactionalPort.execute {
        log.debug("updateSample() - command={}, modifiedBy={}", command, command.modifiedBy)
        if (!samplePort.update(command.id, command.name, command.age, command.status)) {
            throw SampleNotFoundException(command.id)
        }
        samplePort.findById(command.id)!!
    }

    override suspend fun deleteSample(id: Long) {
        transactionalPort.execute {
            log.debug("deleteSample() - id={}", id)
            if (!samplePort.delete(id)) {
                throw SampleNotFoundException(id)
            }
        }
    }
}
