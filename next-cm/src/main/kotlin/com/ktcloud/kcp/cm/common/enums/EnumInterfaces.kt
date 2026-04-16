package com.ktcloud.kcp.cm.common.enums

import com.ktcloud.kcp.cm.common.utils.MessageConverter
import java.util.*

// for database
interface GenericEnum {
    val value: String
}

// for presentation layer
interface DisplayEnum {
    val label: String
    val priority: Int
    val displayable: Boolean

    fun getMessage() = MessageConverter.getMessage(label)
    fun getMessage(locale: Locale) = MessageConverter.getMessage(label, locale)
}
