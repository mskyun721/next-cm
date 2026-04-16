package com.ktcloud.kcp.cm.common.utils

import com.ktcloud.kcp.cm.common.constant.CommonConstant.DEFAULT_LOCALE
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component
import java.util.*

@Component
class MessageConverter(messageSource: MessageSource) {

    init {
        Companion.messageSource = messageSource
    }

    companion object {
        @Volatile
        private var messageSource: MessageSource? = null

        fun getMessage(code: String, locale: Locale = DEFAULT_LOCALE): String = getMessage(code, null, locale, code)

        fun getMessage(
            code: String,
            args: Array<Any>?,
            locale: Locale = DEFAULT_LOCALE,
            defaultMessage: String = code,
        ): String = getMessageOrNull(code, args, locale) ?: defaultMessage

        fun getMessageOrNull(code: String, args: Array<Any>? = null, locale: Locale = DEFAULT_LOCALE): String? =
            checkNotNull(messageSource) {
                "MessageConverter bean이 초기화되지 않았습니다."
            }.getMessage(code, args, null, locale)
    }
}
