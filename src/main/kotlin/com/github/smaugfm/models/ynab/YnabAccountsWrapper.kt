package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccountsWrapper(
    val accounts: List<YnabAccount>,
    val serverKnowledge: Long,
)
