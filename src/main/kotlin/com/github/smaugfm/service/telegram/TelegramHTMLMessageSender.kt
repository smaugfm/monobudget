package com.github.smaugfm.service.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.models.settings.Mappings

class TelegramHTMLMessageSender(
    private val mappings: Mappings,
    private val telegramApi: TelegramApi
) {
    suspend fun send(
        chatId: ChatId,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        telegramApi.sendMessage(
            chatId,
            msg,
            ParseMode.Html,
            markup
        )
    }

    suspend fun send(
        monoAccountId: String,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        this.send(
            mappings.getTelegramChatIdAccByMono(monoAccountId)?.let(ChatId::IntegerId) ?: return,
            msg,
            markup
        )
    }
}
