package com.github.smaugfm.service.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.service.mono.MonoAccountsService

class TelegramMessageSender(
    private val monoAccountsService: MonoAccountsService,
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
            monoAccountsService.getTelegramChatIdAccByMono(monoAccountId)?.let(ChatId::IntegerId) ?: return,
            msg,
            markup
        )
    }
}
