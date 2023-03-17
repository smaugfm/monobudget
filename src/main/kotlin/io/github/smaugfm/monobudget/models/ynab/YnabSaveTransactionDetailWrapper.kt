package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransactionDetailWrapper(
    val transaction: YnabTransactionDetail,
    val transactionIds: List<String>,
    val serverKnowledge: Long
)
