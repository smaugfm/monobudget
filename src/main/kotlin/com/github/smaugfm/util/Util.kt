package com.github.smaugfm.util

import java.util.Currency
import kotlin.math.abs
import kotlin.math.pow

fun Number.formatW(w: Int = 2): String {
    return "%02d".format(this)
}

fun Currency.formatAmount(amount: Long): String {
    val delimiter = (10.0.pow(defaultFractionDigits)).toInt()
    return "${amount / delimiter}.${(abs(amount % delimiter).formatW())}"
}

fun String.replaceNewLines(): String =
    replace("\n", " ").replace("\r", "")
