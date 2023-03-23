package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionsResponse(
    val data: YnabTransactionsDetailWrapper
)
