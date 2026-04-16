package com.ktcloud.kcp.cm.common.enums

import java.time.format.DateTimeFormatter

enum class DatePatternEnum(val value: String, val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(value)) {

    DATE_DEFAULT("yyyy-MM-dd"),
    DATETIME_DEFAULT("yyyy-MM-dd HH:mm:ss"),
    DATETIME_TIME_DELIMITER("yyyy-MM-dd'T'HH:mm:ss"),
}
