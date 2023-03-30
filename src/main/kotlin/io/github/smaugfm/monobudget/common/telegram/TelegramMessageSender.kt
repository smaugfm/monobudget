package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

class TelegramMessageSender : KoinComponent {
    private val monoAccountsService: MonoAccountsService by inject()
    private val telegramApi: TelegramApi by inject()

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
