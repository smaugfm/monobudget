package io.github.smaugfm.monobudget.common.util

import java.math.BigDecimal

fun Int.isEven() = this % 2 == 0
fun Int.isOdd() = !isEven()
