package io.github.smaugfm.monobudget.common.model.settings

import kotlinx.serialization.Serializable

@Serializable
sealed interface AccountSettings {
    val accountId: String
    val alias: String
    val budgetAccountId: String
    val telegramChatId: Long
}
