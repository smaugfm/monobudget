package io.github.smaugfm.monobudget.ynab.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabPayee(
    val id: String,
    val name: String,
    val transferAccountId: String?,
    val deleted: Boolean,
)
