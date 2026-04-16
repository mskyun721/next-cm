package com.ktcloud.kcp.cm.certificate.application.service

import com.ktcloud.kcp.cm.application.port.output.transaction.TransactionalPort
import com.ktcloud.kcp.cm.certificate.application.port.`in`.RegisterCertificateCommand
import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateRepository
import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateValidationPort
import com.ktcloud.kcp.cm.certificate.application.port.out.ParsedCertificate
import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.CertificateStatus
import com.ktcloud.kcp.cm.certificate.domain.ValidationResult
import com.ktcloud.kcp.cm.common.exception.CertificateAlreadyRegisteredException
import com.ktcloud.kcp.cm.common.exception.CertificateNotFoundException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CertificateServiceTest {

    private val transactionalPort: TransactionalPort = mockk()
    private val repository: CertificateRepository = mockk()
    private val validationPort: CertificateValidationPort = mockk()

    private lateinit var service: CertificateService

    @BeforeEach
    fun setUp() {
        service = CertificateService(transactionalPort, repository, validationPort)
        coEveryExecute()
    }

    @Suppress("UNCHECKED_CAST")
    private fun coEveryExecute() {
        val writeBlock = slot<() -> Any?>()
        val readBlock = slot<() -> Any?>()
        coEvery { transactionalPort.execute<Any?>(capture(writeBlock)) } answers { writeBlock.captured.invoke() }
        coEvery { transactionalPort.executeReadOnly<Any?>(capture(readBlock)) } answers { readBlock.captured.invoke() }
    }

    @Test
    fun `register parses pem, persists certificate with VALID status when within validity window`() = runTest {
        val parsed = parsedFixture(
            notBefore = Instant.now().minus(Duration.ofDays(1)),
            notAfter = Instant.now().plus(Duration.ofDays(30)),
        )
        every { validationPort.parse("pem") } returns parsed
        every { repository.findByFingerprint(parsed.fingerprint) } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.register(RegisterCertificateCommand(alias = "alias", pemContent = "pem"))

        assertEquals(CertificateStatus.VALID, result.status)
        assertEquals("alias", result.alias)
        verify { repository.save(any()) }
    }

    @Test
    fun `register throws when fingerprint already exists`() = runTest {
        val parsed = parsedFixture(
            notBefore = Instant.now().minus(Duration.ofDays(1)),
            notAfter = Instant.now().plus(Duration.ofDays(30)),
        )
        every { validationPort.parse("pem") } returns parsed
        every { repository.findByFingerprint(parsed.fingerprint) } returns existingCertificate(parsed)

        assertFailsWith<CertificateAlreadyRegisteredException> {
            service.register(RegisterCertificateCommand(alias = "alias", pemContent = "pem"))
        }
    }

    @Test
    fun `register sets EXPIRED status when notAfter is in the past`() = runTest {
        val parsed = parsedFixture(
            notBefore = Instant.now().minus(Duration.ofDays(10)),
            notAfter = Instant.now().minus(Duration.ofDays(1)),
        )
        every { validationPort.parse("pem") } returns parsed
        every { repository.findByFingerprint(parsed.fingerprint) } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.register(RegisterCertificateCommand(alias = "alias", pemContent = "pem"))

        assertEquals(CertificateStatus.EXPIRED, result.status)
    }

    @Test
    fun `getCertificate throws CertificateNotFoundException when missing`() = runTest {
        val id = UUID.randomUUID()
        every { repository.findById(id) } returns null

        assertFailsWith<CertificateNotFoundException> {
            service.getCertificate(id)
        }
    }

    @Test
    fun `validate updates status to VALID when result is valid`() = runTest {
        val id = UUID.randomUUID()
        val cert = existingCertificate(
            parsedFixture(
                notBefore = Instant.now().minus(Duration.ofDays(1)),
                notAfter = Instant.now().plus(Duration.ofDays(30)),
            ),
        ).copy(id = id, status = CertificateStatus.PENDING)
        every { repository.findById(id) } returns cert
        every { validationPort.validate(cert) } returns ValidationResult.success(30)
        every { repository.updateStatus(id, CertificateStatus.VALID) } returns true

        val result = service.validate(id)

        assertTrue(result.valid)
        verify { repository.updateStatus(id, CertificateStatus.VALID) }
    }

    @Test
    fun `delete throws when row not affected`() = runTest {
        val id = UUID.randomUUID()
        every { repository.delete(id) } returns false

        assertFailsWith<CertificateNotFoundException> {
            service.delete(id)
        }
    }

    @Test
    fun `register sets PENDING status when notBefore is in the future`() = runTest {
        val now = Instant.now()
        val parsed = parsedFixture(
            notBefore = now.plus(Duration.ofDays(1)),
            notAfter = now.plus(Duration.ofDays(30)),
        )
        every { validationPort.parse("pem") } returns parsed
        every { repository.findByFingerprint(parsed.fingerprint) } returns null
        every { repository.save(any()) } answers { firstArg() }

        val result = service.register(RegisterCertificateCommand(alias = "alias", pemContent = "pem"))

        assertEquals(CertificateStatus.PENDING, result.status)
    }

    private fun parsedFixture(notBefore: Instant, notAfter: Instant) = ParsedCertificate(
        subject = "CN=test",
        issuer = "CN=ca",
        serialNumber = "01",
        notBefore = notBefore,
        notAfter = notAfter,
        fingerprint = "FP",
        keyUsage = listOf("digitalSignature"),
        subjectAltNames = listOf("test.example.com"),
    )

    private fun existingCertificate(parsed: ParsedCertificate): Certificate = Certificate(
        id = UUID.randomUUID(),
        alias = "alias",
        subject = parsed.subject,
        issuer = parsed.issuer,
        serialNumber = parsed.serialNumber,
        notBefore = parsed.notBefore,
        notAfter = parsed.notAfter,
        status = CertificateStatus.VALID,
        pemContent = "pem",
        fingerprint = parsed.fingerprint,
        keyUsage = parsed.keyUsage,
        subjectAltNames = parsed.subjectAltNames,
        createdAt = parsed.notBefore,
        updatedAt = parsed.notBefore,
    )
}