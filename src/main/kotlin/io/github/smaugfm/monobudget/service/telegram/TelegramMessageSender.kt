package io.github.smaugfm.monobudget.service.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService

class TelegramMessageSender(
    private val monoAccountsService: MonoAccountsService,
    private val telegramApi: TelegramApi
) {
    suspend fun send(chatId: ChatId, msg: String, markup: ReplyKeyboard? = null) {
        telegramApi.sendMessage(
            chatId,
            msg,
            ParseMode.Html,
            markup
        )
    }

    suspend fun send(monoAccountId: String, newMessage: MessageWithReplyKeyboard) {
        this.send(
            monoAccountsService.getTelegramChatIdAccByMono(monoAccountId)?.let(ChatId::IntegerId) ?: return,
            newMessage.message,
            newMessage.markup
        )
    }
}
