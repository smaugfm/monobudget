package com.github.smaugfm.mono

import kotlinx.serialization.Serializable

@Serializable
data class MonoStatusResponse(
    val status: String,
)
