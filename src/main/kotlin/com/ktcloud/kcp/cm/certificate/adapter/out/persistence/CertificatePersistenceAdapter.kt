package com.ktcloud.kcp.cm.certificate.adapter.out.persistence

import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateRepository
import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.CertificateStatus
import com.ktcloud.kcp.cm.common.extensions.requireByValue
import com.ktcloud.kcp.cm.jooq.generated.tables.Certificates.Companion.CERTIFICATES
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class CertificatePersistenceAdapter(private val dslContext: DSLContext) : CertificateRepository {

    override fun save(certificate: Certificate): Certificate {
        dslContext.insertInto(CERTIFICATES)
            .set(CERTIFICATES.ID, certificate.id)
            .set(CERTIFICATES.ALIAS, certificate.alias)
            .set(CERTIFICATES.SUBJECT, certificate.subject)
            .set(CERTIFICATES.ISSUER, certificate.issuer)
            .set(CERTIFICATES.SERIAL_NUMBER, certificate.serialNumber)
            .set(CERTIFICATES.NOT_BEFORE, toLocalDateTime(certificate.notBefore.toEpochMilli()))
            .set(CERTIFICATES.NOT_AFTER, toLocalDateTime(certificate.notAfter.toEpochMilli()))
            .set(CERTIFICATES.STATUS, certificate.status.value)
            .set(CERTIFICATES.PEM_CONTENT, certificate.pemContent)
            .set(CERTIFICATES.FINGERPRINT, certificate.fingerprint)
            .set(CERTIFICATES.KEY_USAGE, certificate.keyUsage.joinToString(LIST_DELIMITER))
            .set(CERTIFICATES.SUBJECT_ALT_NAMES, certificate.subjectAltNames.joinToString(LIST_DELIMITER))
            .set(CERTIFICATES.CREATED_AT, toLocalDateTime(certificate.createdAt.toEpochMilli()))
            .set(CERTIFICATES.UPDATED_AT, toLocalDateTime(certificate.updatedAt.toEpochMilli()))
            .execute()
        return certificate
    }

    override fun findById(id: UUID): Certificate? = dslContext
        .selectFrom(CERTIFICATES)
        .where(CERTIFICATES.ID.eq(id))
        .fetchOne(::toDomain)

    override fun findByFingerprint(fingerprint: String): Certificate? = dslContext
        .selectFrom(CERTIFICATES)
        .where(CERTIFICATES.FINGERPRINT.eq(fingerprint))
        .fetchOne(::toDomain)

    override fun findAll(): List<Certificate> = dslContext
        .selectFrom(CERTIFICATES)
        .orderBy(CERTIFICATES.CREATED_AT.desc())
        .fetch(::toDomain)

    override fun updateStatus(id: UUID, status: CertificateStatus): Boolean = dslContext
        .update(CERTIFICATES)
        .set(CERTIFICATES.STATUS, status.value)
        .set(CERTIFICATES.UPDATED_AT, toLocalDateTime(System.currentTimeMillis()))
        .where(CERTIFICATES.ID.eq(id))
        .execute() > 0

    override fun delete(id: UUID): Boolean = dslContext
        .deleteFrom(CERTIFICATES)
        .where(CERTIFICATES.ID.eq(id))
        .execute() > 0

    private fun toDomain(record: Record): Certificate = Certificate(
        id = record[CERTIFICATES.ID]!!,
        alias = record[CERTIFICATES.ALIAS]!!,
        subject = record[CERTIFICATES.SUBJECT]!!,
        issuer = record[CERTIFICATES.ISSUER]!!,
        serialNumber = record[CERTIFICATES.SERIAL_NUMBER]!!,
        notBefore = record[CERTIFICATES.NOT_BEFORE]!!.toInstant(ZoneOffset.UTC),
        notAfter = record[CERTIFICATES.NOT_AFTER]!!.toInstant(ZoneOffset.UTC),
        status = requireByValue<CertificateStatus>(record[CERTIFICATES.STATUS]!!),
        pemContent = record[CERTIFICATES.PEM_CONTENT]!!,
        fingerprint = record[CERTIFICATES.FINGERPRINT]!!,
        keyUsage = splitList(record[CERTIFICATES.KEY_USAGE]),
        subjectAltNames = splitList(record[CERTIFICATES.SUBJECT_ALT_NAMES]),
        createdAt = record[CERTIFICATES.CREATED_AT]!!.toInstant(ZoneOffset.UTC),
        updatedAt = record[CERTIFICATES.UPDATED_AT]!!.toInstant(ZoneOffset.UTC),
    )

    private fun splitList(raw: String?): List<String> =
        raw?.takeIf { it.isNotBlank() }?.split(LIST_DELIMITER)?.map(String::trim) ?: emptyList()

    private fun toLocalDateTime(epochMillis: Long): LocalDateTime =
        // WHY: ofInstant preserves sub-second precision; ofEpochSecond with nanos=0 truncates millis.
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)

    companion object {
        private const val LIST_DELIMITER = ","
    }
}
