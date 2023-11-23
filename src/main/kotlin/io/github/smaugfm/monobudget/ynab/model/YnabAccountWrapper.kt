package io.github.smaugfm.monobudget.ynab.model

import kotlinx.serialization.Serializable

@Serializable
data class YnabAccountWrapper(
    val account: YnabAccount,
)
