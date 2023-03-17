package com.github.smaugfm.service.telegram

import com.elbekd.bot.model.ChatId
import com.github.smaugfm.api.TelegramApi

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
