package com.github.smaugfm.mono.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CashbackType() {
    @SerialName("")
    None,
    UAH,
    Miles
}