package io.github.smaugfm.monobudget.common.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveSubTransaction(
    val amount: Long,
    val payeeId: String?,
    val payeeName: String?,
    val categoryId: String?,
    val memo: String?
)
