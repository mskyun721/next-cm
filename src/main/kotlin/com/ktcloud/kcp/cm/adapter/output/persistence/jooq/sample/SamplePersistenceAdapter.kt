package com.ktcloud.kcp.cm.adapter.output.persistence.jooq.sample

import com.ktcloud.kcp.cm.application.port.output.sample.SamplePort
import com.ktcloud.kcp.cm.common.extensions.requireByValue
import com.ktcloud.kcp.cm.domain.sample.model.Sample
import com.ktcloud.kcp.cm.domain.sample.model.SampleStatus
import com.ktcloud.kcp.cm.jooq.generated.tables.Samples.Companion.SAMPLES
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Component

@Component
class SamplePersistenceAdapter(private val dslContext: DSLContext) : SamplePort {

    override fun findAll(): List<Sample> = dslContext
        .selectFrom(SAMPLES)
        .orderBy(SAMPLES.ID.asc())
        .fetch(::toSample)

    override fun findByFilter(name: String?, minAge: Int?, maxAge: Int?, status: SampleStatus?): List<Sample> {
        val conditions = DSL.noCondition()
            .let { c -> name?.let { c.and(SAMPLES.NAME.containsIgnoreCase(it)) } ?: c }
            .let { c -> minAge?.let { c.and(SAMPLES.AGE.ge(it)) } ?: c }
            .let { c -> maxAge?.let { c.and(SAMPLES.AGE.le(it)) } ?: c }
            // GenericEnum.value 기반 필터링: enum → DB value 변환
            .let { c -> status?.let { c.and(SAMPLES.STATUS.eq(it.value)) } ?: c }

        return dslContext
            .selectFrom(SAMPLES)
            .where(conditions)
            .orderBy(SAMPLES.ID.asc())
            .fetch(::toSample)
    }

    // GenericEnum.value 기반 조회: enum → DB value 변환
    override fun findByStatus(status: SampleStatus): List<Sample> = dslContext
        .selectFrom(SAMPLES)
        .where(SAMPLES.STATUS.eq(status.value))
        .orderBy(SAMPLES.ID.asc())
        .fetch(::toSample)

    override fun findById(id: Long): Sample? = dslContext
        .selectFrom(SAMPLES)
        .where(SAMPLES.ID.eq(id))
        .fetchOne(::toSample)

    // GenericEnum.value 기반 저장: enum.value → DB VARCHAR
    override fun insert(name: String, age: Int, status: SampleStatus): Sample = dslContext
        .insertInto(SAMPLES)
        .set(SAMPLES.NAME, name)
        .set(SAMPLES.AGE, age)
        .set(SAMPLES.STATUS, status.value) // ← GenericEnum.value 로 저장
        .returning()
        .fetchSingle(::toSample)

    // GenericEnum.value 기반 수정: enum.value → DB VARCHAR
    override fun update(id: Long, name: String, age: Int, status: SampleStatus): Boolean = dslContext
        .update(SAMPLES)
        .set(SAMPLES.NAME, name)
        .set(SAMPLES.AGE, age)
        .set(SAMPLES.STATUS, status.value) // ← GenericEnum.value 로 저장
        .where(SAMPLES.ID.eq(id))
        .execute() > 0

    override fun delete(id: Long): Boolean = dslContext
        .deleteFrom(SAMPLES)
        .where(SAMPLES.ID.eq(id))
        .execute() > 0

    // DB value → GenericEnum 변환: requireByValue<SampleStatus>(dbValue)
    // 잘못된 DB value → InvalidEnumValueException (DefaultException) 발생
    private fun toSample(record: Record): Sample = Sample(
        id = record[SAMPLES.ID]!!,
        name = record[SAMPLES.NAME]!!,
        age = record[SAMPLES.AGE]!!,
        status = requireByValue<SampleStatus>(record[SAMPLES.STATUS]!!), // ← 공통 유틸 사용
    )
}
