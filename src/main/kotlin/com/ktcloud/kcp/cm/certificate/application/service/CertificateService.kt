package com.ktcloud.kcp.cm.certificate.application.service

import com.ktcloud.kcp.cm.application.port.output.transaction.TransactionalPort
import com.ktcloud.kcp.cm.certificate.application.port.`in`.DeleteCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.GetCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.RegisterCertificateCommand
import com.ktcloud.kcp.cm.certificate.application.port.`in`.RegisterCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.`in`.ValidateCertificateUseCase
import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateRepository
import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateValidationPort
import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.application.port.out.ParsedCertificate
import com.ktcloud.kcp.cm.certificate.domain.CertificateStatus
import com.ktcloud.kcp.cm.certificate.domain.ValidationResult
import com.ktcloud.kcp.cm.common.exception.CertificateAlreadyRegisteredException
import com.ktcloud.kcp.cm.common.exception.CertificateNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class CertificateService(
    private val transactionalPort: TransactionalPort,
    private val repository: CertificateRepository,
    private val validationPort: CertificateValidationPort,
) : RegisterCertificateUseCase,
    GetCertificateUseCase,
    ValidateCertificateUseCase,
    DeleteCertificateUseCase {

    private val log = LoggerFactory.getLogger(this::class.java)

    override suspend fun register(command: RegisterCertificateCommand): Certificate {
        val parsed = validationPort.parse(command.pemContent)
        val now = Instant.now()

        val candidate = buildCertificate(parsed, command, now)
        val initialStatus = decideInitialStatus(candidate, now)
        val toPersist = candidate.copy(status = initialStatus)

        return transactionalPort.execute {
            repository.findByFingerprint(parsed.fingerprint)?.let {
                // WHY: fingerprint uniqueness prevents duplicate logical registrations of the same cert.
                throw CertificateAlreadyRegisteredException(parsed.fingerprint)
            }
            log.debug(
                "register() - alias={}, subject={}, status={}",
                toPersist.alias,
                toPersist.subject,
                toPersist.status,
            )
            repository.save(toPersist)
        }
    }

    override suspend fun getCertificate(id: UUID): Certificate = transactionalPort.executeReadOnly {
        repository.findById(id)
    } ?: throw CertificateNotFoundException(id)

    override suspend fun listCertificates(): List<Certificate> = transactionalPort.executeReadOnly {
        repository.findAll()
    }

    override suspend fun validate(id: UUID): ValidationResult {
        val certificate = transactionalPort.executeReadOnly { repository.findById(id) }
            ?: throw CertificateNotFoundException(id)

        val result = validationPort.validate(certificate)
        val newStatus = statusFromValidation(certificate.status, result, Instant.now(), certificate)

        if (newStatus != certificate.status) {
            transactionalPort.execute { repository.updateStatus(id, newStatus) }
        }
        return result
    }

    override suspend fun delete(id: UUID) {
        transactionalPort.execute {
            if (!repository.delete(id)) {
                throw CertificateNotFoundException(id)
            }
        }
    }

    private fun buildCertificate(
        parsed: ParsedCertificate,
        command: RegisterCertificateCommand,
        now: Instant,
    ): Certificate = Certificate(
        id = UUID.randomUUID(),
        alias = command.alias,
        subject = parsed.subject,
        issuer = parsed.issuer,
        serialNumber = parsed.serialNumber,
        notBefore = parsed.notBefore,
        notAfter = parsed.notAfter,
        status = CertificateStatus.PENDING,
        pemContent = command.pemContent,
        fingerprint = parsed.fingerprint,
        keyUsage = parsed.keyUsage,
        subjectAltNames = parsed.subjectAltNames,
        createdAt = now,
        updatedAt = now,
    )

    private fun decideInitialStatus(certificate: Certificate, now: Instant): CertificateStatus = when {
        certificate.isExpired(now) -> CertificateStatus.EXPIRED
        certificate.isNotYetValid(now) -> CertificateStatus.PENDING
        else -> CertificateStatus.VALID
    }

    private fun statusFromValidation(
        current: CertificateStatus,
        result: ValidationResult,
        now: Instant,
        certificate: Certificate,
    ): CertificateStatus {
        if (current == CertificateStatus.REVOKED) return CertificateStatus.REVOKED
        if (certificate.isExpired(now)) return CertificateStatus.EXPIRED
        // WHY: on validation failure we keep the current status rather than downgrading.
        // Reasoning: a transient CA connectivity issue or intermediate-chain mismatch should not
        // flip a VALID cert to an unknown state. The caller receives the full ValidationResult
        // (including errors) and can decide to act. Deliberate revocation must go through the
        // explicit REVOKED flow, not through a re-validate failure.
        return if (result.valid) CertificateStatus.VALID else current
    }
}
