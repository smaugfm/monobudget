package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

@Serializable
data class MonoStatusResponse(
    val status: String
)