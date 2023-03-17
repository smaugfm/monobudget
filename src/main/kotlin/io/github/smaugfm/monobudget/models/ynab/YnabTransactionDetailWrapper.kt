package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionDetailWrapper(
    val transactionIds: List<String>,
    val transaction: YnabTransactionDetail
)
