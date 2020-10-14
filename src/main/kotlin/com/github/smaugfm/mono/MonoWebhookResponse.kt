package com.github.smaugfm.mono

import kotlinx.serialization.Serializable

@Serializable
data class MonoWebhookResponse(
    val type: String,
    val data: MonoWebHookResponseData,
)

@Serializable
data class MonoWebHookResponseData(
    val account: MonoAccountId,
    val statementItem: MonoStatementItem,
)
