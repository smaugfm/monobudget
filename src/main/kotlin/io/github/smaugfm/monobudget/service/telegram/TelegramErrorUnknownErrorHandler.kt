package io.github.smaugfm.monobudget.service.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.api.TelegramApi

class TelegramErrorUnknownErrorHandler(
    private val telegramChatIds: Set<Long>,
    private val telegramApi: TelegramApi
) {
    suspend operator fun invoke() {
        telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(ChatId.IntegerId(chatId), TelegramApi.UNKNOWN_ERROR_MSG)
            }
    }
}
