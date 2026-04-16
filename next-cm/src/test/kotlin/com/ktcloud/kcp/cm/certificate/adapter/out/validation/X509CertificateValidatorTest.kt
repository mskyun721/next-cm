package com.ktcloud.kcp.cm.certificate.adapter.out.validation

import com.ktcloud.kcp.cm.common.exception.InvalidPemFormatException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * Unit tests for X509CertificateValidator PEM parsing error paths.
 *
 * WHY: full chain validation tests require fixture certificates rooted in the JVM trust store
 * which is environment-dependent; happy-path parsing is covered in integration tests where a
 * generated test certificate (BouncyCastle test util) is wired in.
 */
class X509CertificateValidatorTest {

    private val validator = X509CertificateValidator()

    @Test
    fun `parse throws InvalidPemFormatException when pem body is garbage`() {
        val garbage = "-----BEGIN CERTIFICATE-----\nnot-a-real-cert\n-----END CERTIFICATE-----"
        assertThrows(InvalidPemFormatException::class.java) {
            validator.parse(garbage)
        }
    }

    @Test
    fun `parse throws InvalidPemFormatException on empty input`() {
        assertThrows(InvalidPemFormatException::class.java) { validator.parse("") }
    }

    @Test
    fun `parse throws InvalidPemFormatException on random string`() {
        assertThrows(InvalidPemFormatException::class.java) { validator.parse("hello world") }
    }
}