package com.github.smaugfm.workflows

import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.models.settings.Mappings

class ProcessError(val mappings: Mappings, val telegramApi: TelegramApi) {
    suspend operator fun invoke() {
        mappings
            .getTelegramChatIds()
            .forEach { chatId ->
                telegramApi.sendMessage(chatId, TelegramApi.UNKNOWN_ERROR_MSG)
            }
    }
}
