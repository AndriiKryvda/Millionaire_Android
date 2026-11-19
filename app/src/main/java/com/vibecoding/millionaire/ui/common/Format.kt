package com.vibecoding.millionaire.ui.common

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