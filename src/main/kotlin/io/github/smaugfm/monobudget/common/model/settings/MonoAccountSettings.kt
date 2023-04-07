package io.github.smaugfm.monobudget.common.model.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("mono")
data class MonoAccountSettings(
    override val accountId: String,
    val token: String,
    override val alias: String,
    override val budgetAccountId: String,
    override val telegramChatId: Long
) : AccountSettings
