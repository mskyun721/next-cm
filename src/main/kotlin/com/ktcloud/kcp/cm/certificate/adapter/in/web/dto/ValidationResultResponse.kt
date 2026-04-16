package com.ktcloud.kcp.cm.certificate.adapter.`in`.web.dto

import com.ktcloud.kcp.cm.certificate.domain.ValidationResult

data class ValidationResultResponse(
    val valid: Boolean,
    val errors: List<String>,
    val daysUntilExpiry: Long,
    val revocationChecked: Boolean,
) {
    companion object {
        fun from(result: ValidationResult): ValidationResultResponse = ValidationResultResponse(
            valid = result.valid,
            errors = result.errors,
            daysUntilExpiry = result.daysUntilExpiry,
            revocationChecked = result.revocationChecked,
        )
    }
}
