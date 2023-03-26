package io.github.smaugfm.monobudget.components.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.api.TelegramApi

class TelegramErrorUnknownErrorHandler(
    private val telegramChatIds: List<Long>,
    private val telegramApi: TelegramApi
) {
    suspend operator fun invoke() {
        telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = TelegramApi.UNKNOWN_ERROR_MSG
                )
            }
    }
}
