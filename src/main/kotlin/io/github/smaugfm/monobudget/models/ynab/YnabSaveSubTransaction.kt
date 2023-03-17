package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveSubTransaction(
    val amount: Long,
    val payeeId: String?,
    val payeeName: String?,
    val categoryId: String?,
    val memo: String?
)
