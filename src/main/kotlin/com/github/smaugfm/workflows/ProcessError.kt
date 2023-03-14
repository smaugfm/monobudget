package com.github.smaugfm.workflows

import com.elbekd.bot.model.ChatId
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.models.settings.Mappings

class ProcessError(val mappings: Mappings, val telegramApi: TelegramApi) {
    suspend operator fun invoke() {
        mappings
            .getTelegramChatIds()
            .forEach { chatId ->
                telegramApi.sendMessage(ChatId.IntegerId(chatId), TelegramApi.UNKNOWN_ERROR_MSG)
            }
    }
}
