package io.github.smaugfm.monobudget.ynab.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabBudgetDetailShort(
    val id: String,
    val name: String,
    val currencyFormat: YnabCurrencyFormat,
)
