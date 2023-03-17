package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionResponse(
    val data: YnabTransactionDetailWrapper
)
