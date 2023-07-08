package io.github.smaugfm.monobudget.common.model.financial

import io.github.smaugfm.monobudget.common.util.formatW
import io.github.smaugfm.monobudget.common.util.toHumanReadable
import java.math.BigDecimal
import java.util.Currency
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * In minimum currency units (e.g. 1/100th of a dollar)
 */
class Amount(val value: Long, val currency: Currency) {
    private val multiplier = 10.0.pow(currency.defaultFractionDigits).toInt()

    fun formatShort(): String {
        val formatted = (value / multiplier).toHumanReadable()
        val symbol = if (currency.currencyCode == "UAH") "₴" else currency.symbol
        if (formatted.startsWith('-')) {
            return "-$symbol${formatted.substring(1)}"
        }
        return "$symbol${(value / multiplier).toHumanReadable()}"
    }

    fun format(): String =
        "${value / multiplier}.${(abs(value % multiplier).formatW())} ${currency.currencyCode}"

    override fun toString(): String {
        return format()
    }

    operator fun unaryMinus() = Amount(-value, currency)

    fun equalsInverted(other: Amount): Boolean = this.value == -other.value && currency == other.currency

    /**
     * Monobank amount uses minimum currency units (e.g. cents for dollars)
     * and YNAB amount uses milliunits (1/1000th of a dollar)
     */
    fun toYnabAmountLong() = value * (YNAB_MILLI_MILTIPLIER / currency.defaultFractionDigits)

    fun toLunchmoneyAmountBigDecimal() = value.toBigDecimal().setScale(currency.defaultFractionDigits) /
        multiplier.toBigDecimal()

    companion object {

        fun fromYnabAmount(ynabAmount: Long, currency: Currency) =
            Amount(ynabAmount / (YNAB_MILLI_MILTIPLIER / currency.defaultFractionDigits), currency)

        fun fromLunchmoneyAmount(lunchmoneyAmount: Double, currency: Currency) = Amount(
            (lunchmoneyAmount * (BigDecimal.TEN.pow(currency.defaultFractionDigits)).toLong()).roundToLong(),
            currency
        )

        private const val YNAB_MILLI_MILTIPLIER = 1000
    }
}
