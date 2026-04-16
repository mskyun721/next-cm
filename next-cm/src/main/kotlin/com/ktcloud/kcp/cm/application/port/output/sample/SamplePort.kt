package com.ktcloud.kcp.cm.application.port.output.sample

import com.ktcloud.kcp.cm.domain.sample.model.Sample
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus

interface SamplePort {
    fun findAll(): List<Sample>
    fun findByFilter(name: String?, minAge: Int?, maxAge: Int?, status: SampleStatus?): List<Sample>
    fun findByStatus(status: SampleStatus): List<Sample>
    fun findById(id: Long): Sample?
    fun insert(name: String, age: Int, status: SampleStatus): Sample
    fun update(id: Long, name: String, age: Int, status: SampleStatus): Boolean
    fun delete(id: Long): Boolean
}
