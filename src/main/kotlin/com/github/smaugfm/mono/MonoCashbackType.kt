package com.github.smaugfm.mono

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MonoCashbackType {
    @SerialName("")
    None,
    UAH,
    Miles
}
