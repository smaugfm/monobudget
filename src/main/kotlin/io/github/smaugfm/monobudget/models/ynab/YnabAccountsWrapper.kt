package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccountsWrapper(
    val accounts: List<YnabAccount>,
    val serverKnowledge: Long
)
