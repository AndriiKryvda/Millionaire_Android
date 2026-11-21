package com.vibecoding.millionaire.ui.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Groups digits with a non-breaking space: 1000 -> "1 000". */
fun formatGroups(amount: Int): String {
    val digits = kotlin.math.abs(amount).toString()
    val builder = StringBuilder()
    digits.reversed().forEachIndexed { index, char ->
        if (index > 0 && index % 3 == 0) builder.append(' ')
        builder.append(char)
    }
    return (if (amount < 0) "-" else "") + builder.reverse()
}

/** Formats an epoch-millis timestamp as `DD.MM.YYYY` in the device time zone. */
@OptIn(ExperimentalTime::class)
fun formatDate(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val date = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val day = date.day.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day.$month.${date.year}"
}
