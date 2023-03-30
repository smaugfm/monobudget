package io.github.smaugfm.monobudget.common.util

import java.math.BigDecimal

fun BigDecimal.isPositive() = this.signum() == 1
fun BigDecimal.isZero() = this.signum() == 0
fun BigDecimal.isNotZero() = !isZero()
