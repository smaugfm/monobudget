package io.github.smaugfm.monobudget.common.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSubTransaction(
    val id: String,
    val transactionId: String,
    val amount: Long,
    val memo: String?,
    val payeeId: String?,
    val payeeName: String?,
    val categoryId: String?,
    val categoryName: String?,
    val transferAccountId: String?,
    val transferTransactionId: String?,
    val delete: Boolean = false
)
