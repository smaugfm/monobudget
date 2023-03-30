package io.github.smaugfm.monobudget.common.telegram

import com.elbekd.bot.model.ChatId
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class TelegramErrorUnknownErrorHandler : KoinComponent {
    private val monoSettings: io.github.smaugfm.monobudget.common.model.Settings.MultipleMonoSettings by inject()
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
