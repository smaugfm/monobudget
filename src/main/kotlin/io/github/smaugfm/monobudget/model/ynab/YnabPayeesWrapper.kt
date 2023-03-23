package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabPayeesWrapper(
    val payees: List<YnabPayee>
)
