package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionsDetailWrapper(
    val transactions: List<YnabTransactionDetail>,
    val serverKnowledge: Long,
)
