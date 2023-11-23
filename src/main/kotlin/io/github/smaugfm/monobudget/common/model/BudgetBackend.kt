package io.github.smaugfm.monobudget.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BudgetBackend {
    abstract val token: String

    @Serializable
    @SerialName("ynab")
    data class YNAB(
        override val token: String,
        val ynabBudgetId: String,
    ) : BudgetBackend()

    @Serializable
    @SerialName("lunchmoney")
    data class Lunchmoney(
        override val token: String,
        val transferCategoryId: String,
    ) : BudgetBackend()
}
