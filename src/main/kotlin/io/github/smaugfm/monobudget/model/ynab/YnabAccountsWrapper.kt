package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccountsWrapper(
    val accounts: List<YnabAccount>
)
