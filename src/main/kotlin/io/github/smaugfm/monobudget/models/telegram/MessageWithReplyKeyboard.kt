package io.github.smaugfm.monobudget.models.telegram

import com.elbekd.bot.types.ReplyKeyboard

data class MessageWithReplyKeyboard(
    val message: String,
    val markup: ReplyKeyboard
)
