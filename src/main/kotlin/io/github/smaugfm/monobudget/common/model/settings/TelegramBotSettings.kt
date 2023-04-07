package io.github.smaugfm.monobudget.common.model.settings

import kotlinx.serialization.Serializable

@Serializable
data class TelegramBotSettings(
    val token: String,
    val username: String
)
