package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

@Serializable
data class WebHookRequest(
    val webHookUrl: String
)
