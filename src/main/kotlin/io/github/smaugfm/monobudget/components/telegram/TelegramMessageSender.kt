package io.github.smaugfm.monobudget.components.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.model.telegram.MessageWithReplyKeyboard
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class TelegramMessageSender(
    private val monoAccountsService: MonoAccountsService,
    private val telegramApi: TelegramApi
) {
    suspend fun send(monoAccountId: String, newMessage: MessageWithReplyKeyboard) {
        val chatId = monoAccountsService.getTelegramChatIdAccByMono(monoAccountId)
        if (chatId == null) {
            log.error { "Failed to map Monobank account to telegram chat id. Account: $monoAccountId" }
            return
        }

        this.send(
            ChatId.IntegerId(chatId),
            newMessage.message,
            newMessage.notifyTelegramApp,
            newMessage.markup
        )
    }

    private suspend fun send(chatId: ChatId, msg: String, notify: Boolean, markup: ReplyKeyboard? = null) {
        telegramApi.sendMessage(
            chatId = chatId,
            text = msg,
            parseMode = ParseMode.Html,
            disableNotification = !notify,
            replyMarkup = markup
        )
    }
}
