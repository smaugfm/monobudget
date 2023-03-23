package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabAccountType {
    @SerialName("checking")
    Checking,

    @SerialName("savings")
    Savings,

    @SerialName("cash")
    Cash,

    @SerialName("creditCard")
    CreditCard,

    @SerialName("lineOfCredit")
    LineOfCredit,

    @SerialName("otherAsset")
    OtherAsset,

    @SerialName("otherLiability")
    OtherLiability,

    @SerialName("payPal")
    PayPal,

    @SerialName("merchantAccount")
    MerchantAccount,

    @SerialName("investmentAccount")
    InvestmentAccount,

    @SerialName("mortgage")
    Mortgage
}
