package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabFlagColor {
    @SerialName("red")
    Red,

    @SerialName("orange")
    Orange,

    @SerialName("yellow")
    Yellow,

    @SerialName("green")
    Green,

    @SerialName("blue")
    Blue,

    @SerialName("purple")
    Purple
}
