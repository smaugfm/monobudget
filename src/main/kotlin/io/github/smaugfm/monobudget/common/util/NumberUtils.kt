package io.github.smaugfm.monobudget.common.util

import java.text.StringCharacterIterator
import java.util.Locale

fun Int.isEven() = this % 2 == 0

fun Int.isOdd() = !isEven()

@Suppress("MagicNumber")
fun Long.toHumanReadable(): String {
    var num = this
    if (-1000 < num && num < 1000) {
        return "$num"
    }
    val ci = StringCharacterIterator("kMGTPE")
    while (num <= -999950 || num >= 999950) {
        num /= 1000
        ci.next()
    }
    val resultNumber = num / 1000.0
    return if (resultNumber == resultNumber.toLong().toDouble()) {
        String.format(Locale.getDefault(), "%d%c", resultNumber.toLong(), ci.current())
    } else {
        String.format(Locale.getDefault(), "%.1f%c", resultNumber, ci.current())
    }
}
