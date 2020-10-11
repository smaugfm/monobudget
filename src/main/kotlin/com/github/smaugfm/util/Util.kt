package com.github.smaugfm.util

import java.util.*
import kotlin.math.pow

fun Currency.formatAmount(amount: Long): String {
    val delimiter = (10.0.pow(defaultFractionDigits)).toInt()
    return "${amount / delimiter}.${amount % delimiter}"
}