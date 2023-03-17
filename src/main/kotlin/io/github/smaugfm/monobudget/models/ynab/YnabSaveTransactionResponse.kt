package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransactionResponse(
    val data: YnabSaveTransactionDetailWrapper
)
