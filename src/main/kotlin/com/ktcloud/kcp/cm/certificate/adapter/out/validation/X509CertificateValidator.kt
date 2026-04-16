package com.ktcloud.kcp.cm.certificate.adapter.out.validation

import com.ktcloud.kcp.cm.certificate.application.port.out.CertificateValidationPort
import com.ktcloud.kcp.cm.certificate.application.port.out.ParsedCertificate
import com.ktcloud.kcp.cm.certificate.domain.Certificate
import com.ktcloud.kcp.cm.certificate.domain.ValidationResult
import com.ktcloud.kcp.cm.common.exception.InvalidPemFormatException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertPathValidator
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.PKIXParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

@Component
class X509CertificateValidator : CertificateValidationPort {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X.509")

    // WHY: building PKIX params is non-trivial; cache trust anchors once at startup.
    private val trustAnchors: Set<TrustAnchor> by lazy { loadSystemTrustAnchors() }

    override fun parse(pemContent: String): ParsedCertificate {
        val x509 = parseX509(pemContent)
        return ParsedCertificate(
            subject = x509.subjectX500Principal.name,
            issuer = x509.issuerX500Principal.name,
            serialNumber = x509.serialNumber.toString(SERIAL_RADIX),
            notBefore = x509.notBefore.toInstant(),
            notAfter = x509.notAfter.toInstant(),
            fingerprint = sha256Fingerprint(x509.encoded),
            keyUsage = extractKeyUsage(x509),
            subjectAltNames = extractSubjectAltNames(x509),
        )
    }

    override fun validate(certificate: Certificate): ValidationResult {
        val errors = mutableListOf<String>()
        val now = Instant.now()
        val daysUntilExpiry = Duration.between(now, certificate.notAfter).toDays()

        val x509 = try {
            parseX509(certificate.pemContent)
        } catch (ex: InvalidPemFormatException) {
            return ValidationResult.failure(
                errors = listOf("PEM parse error: ${ex.message}"),
                daysUntilExpiry = daysUntilExpiry,
                // WHY: revocation check is skipped in Phase 1 (ADR-004); always false here.
                revocationChecked = false,
            )
        }

        validatePeriod(x509, now, errors)
        validateChain(x509, errors)

        return if (errors.isEmpty()) {
            ValidationResult.success(daysUntilExpiry, revocationChecked = false)
        } else {
            ValidationResult.failure(errors, daysUntilExpiry, revocationChecked = false)
        }
    }

    private fun parseX509(pemContent: String): X509Certificate = try {
        val bytes = pemContent.toByteArray(Charsets.UTF_8)
        ByteArrayInputStream(bytes).use { stream ->
            certificateFactory.generateCertificate(stream) as X509Certificate
        }
    } catch (ex: CertificateException) {
        throw InvalidPemFormatException("Cannot parse X.509 certificate", ex)
    } catch (ex: ClassCastException) {
        throw InvalidPemFormatException("PEM is not an X.509 certificate", ex)
    }

    private fun validatePeriod(x509: X509Certificate, now: Instant, errors: MutableList<String>) {
        try {
            x509.checkValidity(java.util.Date.from(now))
        } catch (ex: CertificateException) {
            errors.add("Validity period check failed: ${ex.message}")
        }
    }

    private fun validateChain(x509: X509Certificate, errors: MutableList<String>) {
        try {
            val anchors = trustAnchors
            if (anchors.isEmpty()) {
                // WHY: empty trust store means no CA is trusted — all chain validations will fail.
                // This indicates a JVM configuration problem and should be investigated immediately.
                log.warn("validateChain() - trust store is empty; chain validation cannot proceed")
                errors.add("Trust store is empty: cannot validate chain")
                return
            }
            val params = PKIXParameters(anchors).apply {
                // WHY: CRL/OCSP checks are out of scope (PRD Phase 2).
                isRevocationEnabled = false
            }
            val certPath = certificateFactory.generateCertPath(listOf(x509))
            CertPathValidator.getInstance("PKIX").validate(certPath, params)
        } catch (ex: java.security.GeneralSecurityException) {
            errors.add("Chain validation failed: ${ex.message ?: ex::class.simpleName}")
            log.debug("validateChain() - chain validation failed", ex)
        }
    }

    private fun loadSystemTrustAnchors(): Set<TrustAnchor> {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        // WHY: null initializes with the default JVM cacerts trust store.
        tmf.init(null as KeyStore?)
        val trustManager = tmf.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: return emptySet()
        return trustManager.acceptedIssuers.map { TrustAnchor(it, null) }.toSet()
    }

    private fun sha256Fingerprint(der: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(der)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun extractKeyUsage(x509: X509Certificate): List<String> {
        val raw = x509.keyUsage ?: return emptyList()
        return KEY_USAGE_LABELS.filterIndexed { index, _ -> index < raw.size && raw[index] }
    }

    private fun extractSubjectAltNames(x509: X509Certificate): List<String> {
        val names = try {
            x509.subjectAlternativeNames
        } catch (_: CertificateException) {
            null
        } ?: return emptyList()
        return names.mapNotNull { entry ->
            entry.getOrNull(SAN_VALUE_INDEX)?.toString()
        }
    }

    companion object {
        private const val SERIAL_RADIX = 16
        private const val SAN_VALUE_INDEX = 1
        private val KEY_USAGE_LABELS = listOf(
            "digitalSignature",
            "nonRepudiation",
            "keyEncipherment",
            "dataEncipherment",
            "keyAgreement",
            "keyCertSign",
            "cRLSign",
            "encipherOnly",
            "decipherOnly",
        )
    }
}
