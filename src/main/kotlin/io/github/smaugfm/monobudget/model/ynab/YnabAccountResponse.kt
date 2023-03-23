package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccountResponse(
    val data: YnabAccountWrapper
)
