package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransactionResponse(
    val data: YnabSaveTransactionDetailWrapper,
)
