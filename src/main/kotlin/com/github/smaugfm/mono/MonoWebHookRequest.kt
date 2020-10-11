package com.github.smaugfm.mono

import kotlinx.serialization.Serializable

@Serializable
data class MonoWebHookRequest(
    val webHookUrl: String,
)
