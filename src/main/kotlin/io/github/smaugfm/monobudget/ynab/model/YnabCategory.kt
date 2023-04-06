package io.github.smaugfm.monobudget.ynab.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabCategory(
    val id: String,
    val categoryGroupId: String,
    val name: String,
    val hidden: Boolean,
    val originalCategoryGroupId: String?,
    val note: String?,
    val budgeted: Long,
    val activity: Long,
    val balance: Long,
    val goalType: String?,
    val goalCreationMonth: String?,
    val goalTarget: Long?,
    val goalTargetMonth: String?,
    val goalPercentageComplete: Int?,
    val deleted: Boolean
)
