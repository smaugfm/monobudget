package io.github.smaugfm.monobudget.common.model.financial

import java.util.Currency

data class Account(
    val id: String,
    val alias: String,
    val currency: Currency,
)
