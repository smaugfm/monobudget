package io.github.smaugfm.monobudget.models.ynab

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabFlagColor {
    @SerializedName("red")
    Red,

    @SerializedName("orange")
    Orange,

    @SerializedName("yellow")
    Yellow,

    @SerializedName("green")
    Green,

    @SerializedName("blue")
    Blue,

    @SerializedName("purple")
    Purple
}
