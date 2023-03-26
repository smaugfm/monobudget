package io.github.smaugfm.monobudget.model.telegram

import com.elbekd.bot.types.ReplyKeyboard

data class MessageWithReplyKeyboard(
    val message: String,
    val markup: ReplyKeyboard,
    val notifyTelegramApp: Boolean
)
