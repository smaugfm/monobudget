package io.github.smaugfm.monobudget.common.notify

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.github.smaugfm.monobudget.common.model.telegram.MessageWithReplyKeyboard
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class TelegramNotificationSender(
    private val bankAccounts: BankAccountService,
    private val telegramApi: TelegramApi,
) : StatementItemNotificationSender {
    override suspend fun notify(
        accountId: BankAccountId,
        newMessage: MessageWithReplyKeyboard,
    ) {
        val chatId = bankAccounts.getTelegramChatIdByAccountId(accountId)
        if (chatId == null) {
            log.error { "Failed to map bank account id to telegram chat id. Account: $accountId" }
            return
        }

        log.info { "Sending message to telegramChatId=$chatId. bankAccountId=$accountId)" }
        telegramApi.sendMessage(
            chatId = ChatId.IntegerId(chatId),
            text = newMessage.message,
            parseMode = ParseMode.Html,
            disableNotification = !newMessage.notifyTelegramApp,
            replyMarkup = newMessage.markup,
        )
    }
}
