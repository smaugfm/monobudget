package io.github.smaugfm.monobudget.common.util

fun Int.isEven() = this % 2 == 0
fun Int.isOdd() = !isEven()
