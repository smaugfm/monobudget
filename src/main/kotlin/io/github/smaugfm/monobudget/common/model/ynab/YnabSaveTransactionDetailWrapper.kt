package io.github.smaugfm.monobudget.common.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransactionDetailWrapper(
    val transaction: YnabTransactionDetail
)
