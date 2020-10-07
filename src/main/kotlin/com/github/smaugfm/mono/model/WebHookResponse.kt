package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable


@Serializable
data class WebHookResponse(
    val type: String,
    val data: WebHookResponseData
)

@Serializable
data class WebHookResponseData(
    val account: AccountId,
    val statementItem: StatementItem
)