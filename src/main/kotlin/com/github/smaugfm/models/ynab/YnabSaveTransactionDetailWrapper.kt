package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransactionDetailWrapper(
    val transaction: YnabTransactionDetail,
    val transactionIds: List<String>,
    val serverKnowledge: Long,
)
