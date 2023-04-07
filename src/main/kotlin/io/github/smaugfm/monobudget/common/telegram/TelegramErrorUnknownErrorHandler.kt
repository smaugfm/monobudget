package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import org.koin.core.annotation.Single

@Single
class TelegramErrorUnknownErrorHandler(
    private val monoSettings: MultipleAccountSettings,
    private val telegramApi: TelegramApi
) {
    suspend operator fun invoke() {
        monoSettings.telegramChatIds
            .forEach { chatId ->
                telegramApi.sendMessage(
                    chatId = ChatId.IntegerId(chatId),
                    text = TelegramApi.UNKNOWN_ERROR_MSG
                )
            }
    }
}
