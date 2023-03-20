package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionsDetailWrapper(
    val transactions: List<YnabTransactionDetail>,
)
