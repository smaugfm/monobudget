package io.github.smaugfm.monobudget.common.model.settings

import io.github.smaugfm.monobudget.common.model.financial.Account
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
sealed interface AccountSettings {
    val accountId: String
    val alias: String
    val budgetAccountId: String
    val telegramChatId: Long
    val currency: Currency
}

fun AccountSettings.toMonoAccount() = Account(accountId, alias, currency)
