package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionDetailWrapper(
    val transaction: YnabTransactionDetail
)
