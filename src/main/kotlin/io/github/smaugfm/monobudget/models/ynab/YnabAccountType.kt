package io.github.smaugfm.monobudget.models.ynab

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabAccountType {
    @SerializedName("checking")
    Checking,

    @SerializedName("savings")
    Savings,

    @SerializedName("cash")
    Cash,

    @SerializedName("creditCard")
    CreditCard,

    @SerializedName("lineOfCredit")
    LineOfCredit,

    @SerializedName("otherAsset")
    OtherAsset,

    @SerializedName("otherLiability")
    OtherLiability,

    @SerializedName("payPal")
    PayPal,

    @SerializedName("merchantAccount")
    MerchantAccount,

    @SerializedName("investmentAccount")
    InvestmentAccount,

    @SerializedName("mortgage")
    Mortgage
}
