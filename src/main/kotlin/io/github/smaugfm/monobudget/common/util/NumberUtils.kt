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
    return String.format(Locale.getDefault(), "%.1f%c", num / 1000.0, ci.current())
}
