package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class TelegramMessageSender(
    private val accounts: AccountsService,
    private val telegramApi: TelegramApi
) {

    suspend fun send(accountId: String, newMessage: MessageWithReplyKeyboard) {
        val chatId = accounts.getTelegramChatIdByAccountId(accountId)
        if (chatId == null) {
            log.error { "Failed to map Monobank account to telegram chat id. Account: $accountId" }
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
