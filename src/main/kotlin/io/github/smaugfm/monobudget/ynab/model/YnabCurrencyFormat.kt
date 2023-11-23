package io.github.smaugfm.monobudget.ynab.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabCurrencyFormat(
    val isoCode: String,
    val exampleFormat: String,
    val decimalDigits: Int,
    val decimalSeparator: String,
    val symbolFirst: Boolean,
    val groupSeparator: String,
    val currencySymbol: String,
    val displaySymbol: String,
)
