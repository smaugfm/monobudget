package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabPayeesWrapper(
    val payees: List<YnabPayee>,
    val serverKnowledge: Long,
)
