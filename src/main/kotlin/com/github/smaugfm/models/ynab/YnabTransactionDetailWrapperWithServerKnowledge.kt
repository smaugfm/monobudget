package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionDetailWrapperWithServerKnowledge(
    val transactionIds: List<String>,
    val transaction: YnabTransactionDetail,
    val serverKnowledge: Long,
)
