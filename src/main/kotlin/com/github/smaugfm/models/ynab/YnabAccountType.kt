@file:Suppress("EnumEntryName")

package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
enum class YnabAccountType {
    checking,
    savings,
    cash,
    creditCard,
    lineOfCredit,
    otherAsset,
    otherLiability,
    payPal,
    merchantAccount,
    investmentAccount,
    mortgage
}
