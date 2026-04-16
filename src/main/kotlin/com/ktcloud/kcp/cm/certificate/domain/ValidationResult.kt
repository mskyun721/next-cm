package com.ktcloud.kcp.cm.certificate.domain

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val daysUntilExpiry: Long,
    // WHY: revocation check (CRL/OCSP) is disabled in Phase 1 (PRD ADR-004).
    // Clients must know whether revocation was checked so they can act accordingly.
    val revocationChecked: Boolean,
) {
    companion object {
        fun success(daysUntilExpiry: Long, revocationChecked: Boolean = false): ValidationResult =
            ValidationResult(
                valid = true,
                errors = emptyList(),
                daysUntilExpiry = daysUntilExpiry,
                revocationChecked = revocationChecked,
            )

        fun failure(
            errors: List<String>,
            daysUntilExpiry: Long,
            revocationChecked: Boolean = false,
        ): ValidationResult =
            ValidationResult(
                valid = false,
                errors = errors,
                daysUntilExpiry = daysUntilExpiry,
                revocationChecked = revocationChecked,
            )
    }
}
