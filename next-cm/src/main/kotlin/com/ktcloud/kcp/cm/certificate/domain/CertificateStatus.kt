package com.ktcloud.kcp.cm.certificate.domain

import com.ktcloud.kcp.cm.common.enums.DisplayEnum
import com.ktcloud.kcp.cm.common.enums.GenericEnum

enum class CertificateStatus(
    override val value: String,
    override val label: String,
    override val priority: Int,
    override val displayable: Boolean = true,
) : GenericEnum, DisplayEnum {
    PENDING("pending", "enum.CertificateStatus.PENDING", 1),
    VALID("valid", "enum.CertificateStatus.VALID", 2),
    EXPIRED("expired", "enum.CertificateStatus.EXPIRED", 3),
    REVOKED("revoked", "enum.CertificateStatus.REVOKED", 4),
}
