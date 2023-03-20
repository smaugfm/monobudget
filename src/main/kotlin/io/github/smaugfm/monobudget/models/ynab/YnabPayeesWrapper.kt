package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabPayeesWrapper(
    val payees: List<YnabPayee>
)
