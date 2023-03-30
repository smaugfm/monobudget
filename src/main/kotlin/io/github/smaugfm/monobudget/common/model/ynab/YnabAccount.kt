package io.github.smaugfm.monobudget.common.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccount(
    val id: String,
    val name: String,
    val type: YnabAccountType,
    val onBudget: Boolean,
    val closed: Boolean,
    val note: String?,
    val balance: Long,
    val clearedBalance: Long,
    val unclearedBalance: Long,
    val transferPayeeId: String,
    val directImportLinked: Boolean,
    val directImportInError: Boolean,
    val deleted: Boolean
)
