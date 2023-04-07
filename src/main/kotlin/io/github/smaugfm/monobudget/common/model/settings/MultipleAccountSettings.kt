package io.github.smaugfm.monobudget.common.model.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MultipleAccountSettings(
    val settings: List<AccountSettings>
) {
    @Transient
    val byId = settings.associateBy { it.accountId }

    @Transient
    val accountIds = settings.map { it.accountId }

    @Transient
    val telegramChatIds = settings.map { it.telegramChatId }
}
