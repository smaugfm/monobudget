package com.github.smaugfm.service.telegram

import com.elbekd.bot.model.ChatId
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.models.settings.Mappings

class TelegramErrorUnknownErrorHandler(private val mappings: Mappings, private val telegramApi: TelegramApi) {
    suspend operator fun invoke() {
        mappings
            .getTelegramChatIds()
            .forEach { chatId ->
                telegramApi.sendMessage(ChatId.IntegerId(chatId), TelegramApi.UNKNOWN_ERROR_MSG)
            }
    }
}
