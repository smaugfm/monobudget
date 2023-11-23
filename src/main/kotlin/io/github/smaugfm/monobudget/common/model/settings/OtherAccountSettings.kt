package io.github.smaugfm.monobudget.common.model.settings

import io.github.smaugfm.monobudget.common.model.serializer.CurrencyAsStringSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
@SerialName("other")
data class OtherAccountSettings(
    override val accountId: String,
    override val alias: String,
    override val budgetAccountId: String,
    override val telegramChatId: Long,
    @Serializable(CurrencyAsStringSerializer::class)
    override val currency: Currency,
) : AccountSettings
