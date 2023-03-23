package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabBudgetDetailShort(
    val id: String,
    val name: String,
    val currencyFormat: YnabCurrencyFormat
)
