package io.github.smaugfm.monobudget.components.telegram

import com.elbekd.bot.model.ChatId
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.model.Settings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TelegramErrorUnknownErrorHandler : KoinComponent {
    private val monoSettings: Settings.MultipleMonoSettings by inject()
    private val telegramApi: TelegramApi by inject()

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
