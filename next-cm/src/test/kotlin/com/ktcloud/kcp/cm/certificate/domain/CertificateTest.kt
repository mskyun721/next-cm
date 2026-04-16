package com.ktcloud.kcp.cm.certificate.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CertificateTest {

    @Test
    fun `isExpired returns true when now is after notAfter`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.minus(Duration.ofDays(30)), notAfter = now.minus(Duration.ofDays(1)))
        assertTrue(cert.isExpired(now))
    }

    @Test
    fun `isNotYetValid returns true when now is before notBefore`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.plus(Duration.ofDays(1)), notAfter = now.plus(Duration.ofDays(30)))
        assertTrue(cert.isNotYetValid(now))
        assertFalse(cert.isExpired(now))
    }

    @Test
    fun `daysUntilExpiry computes whole days to notAfter`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now, notAfter = now.plus(Duration.ofDays(10)))
        assertEquals(10L, cert.daysUntilExpiry(now))
    }

    @Test
    fun `withStatus returns a copy with new status and updatedAt`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.minusSeconds(1), notAfter = now.plus(Duration.ofDays(10)))
        val updated = cert.withStatus(CertificateStatus.REVOKED, now.plusSeconds(60))
        assertEquals(CertificateStatus.REVOKED, updated.status)
        assertEquals(now.plusSeconds(60), updated.updatedAt)
        assertEquals(CertificateStatus.VALID, cert.status)
    }

    @Test
    fun `isExpired returns false when now equals notAfter (boundary)`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.minus(Duration.ofDays(30)), notAfter = now)
        assertFalse(cert.isExpired(now))
    }

    @Test
    fun `isExpired returns true when now is 1 millisecond after notAfter (boundary)`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.minus(Duration.ofDays(30)), notAfter = now)
        assertTrue(cert.isExpired(now.plusMillis(1)))
    }

    @Test
    fun `isNotYetValid returns false when now equals notBefore (boundary)`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now, notAfter = now.plus(Duration.ofDays(30)))
        assertFalse(cert.isNotYetValid(now))
    }

    @Test
    fun `isNotYetValid returns true when now is 1 millisecond before notBefore (boundary)`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now, notAfter = now.plus(Duration.ofDays(30)))
        assertTrue(cert.isNotYetValid(now.minusMillis(1)))
    }

    @Test
    fun `daysUntilExpiry returns 0 when less than 1 full day remains`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now, notAfter = now.plus(Duration.ofHours(23)))
        assertEquals(0L, cert.daysUntilExpiry(now))
    }

    @Test
    fun `daysUntilExpiry returns negative when already expired`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        val cert = fixture(notBefore = now.minus(Duration.ofDays(10)), notAfter = now.minus(Duration.ofDays(1)))
        assertTrue(cert.daysUntilExpiry(now) < 0)
    }

    @Test
    fun `daysUntilExpiry truncates to whole days`() {
        val now = Instant.parse("2030-01-01T00:00:00Z")
        // 10 days + 12 hours
        val cert = fixture(notBefore = now, notAfter = now.plus(Duration.ofDays(10)).plus(Duration.ofHours(12)))
        assertEquals(10L, cert.daysUntilExpiry(now))
    }

    private fun fixture(notBefore: Instant, notAfter: Instant): Certificate = Certificate(
        id = UUID.randomUUID(),
        alias = "test",
        subject = "CN=test",
        issuer = "CN=ca",
        serialNumber = "01",
        notBefore = notBefore,
        notAfter = notAfter,
        status = CertificateStatus.VALID,
        pemContent = "-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----",
        fingerprint = "00",
        keyUsage = emptyList(),
        subjectAltNames = emptyList(),
        createdAt = notBefore,
        updatedAt = notBefore,
    )
}