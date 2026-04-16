package com.ktcloud.kcp.cm.common.extensions

import com.ktcloud.kcp.cm.common.enums.DatePatternEnum
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val LAST_MINUTE_OF_HOUR = 59
private const val LAST_SECOND_OF_MINUTE = 59
private val END_OF_DAY_TIME: LocalTime = LocalTime.MAX.withNano(0)

fun LocalDateTime.atStartOfHour(): LocalDateTime = with(toLocalTime().withMinute(0).withSecond(0).withNano(0))

fun LocalDateTime.atEndOfHour(): LocalDateTime =
    with(LocalTime.of(this.toLocalTime().hour, LAST_MINUTE_OF_HOUR, LAST_SECOND_OF_MINUTE))

fun LocalDateTime.atEndOfDay(): LocalDateTime = with(END_OF_DAY_TIME)

fun LocalDateTime.atMidNight(): LocalDateTime = with(LocalTime.MIDNIGHT)

fun LocalDate.atEndOfDay(): LocalDateTime = LocalDateTime.of(this, END_OF_DAY_TIME)

fun LocalDateTime.setTime(hour: Int, minute: Int, second: Int): LocalDateTime =
    this.with(LocalTime.of(hour, minute, second))

fun LocalDateTime.toString(datePattern: DatePatternEnum): String = this.format(datePattern.formatter)

fun LocalDate.toString(datePattern: DatePatternEnum): String = this.format(datePattern.formatter)
