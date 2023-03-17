package io.github.smaugfm.monobudget.models.ynab

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabCleared {
    @SerializedName("Cleared")
    Cleared,

    @SerializedName("uncleared")
    Uncleared,

    @SerializedName("reconciled")
    Reconciled
}
