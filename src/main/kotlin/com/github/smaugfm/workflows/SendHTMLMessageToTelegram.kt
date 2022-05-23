package com.github.smaugfm.workflows

import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.models.MonoAccountId
import com.github.smaugfm.models.settings.Mappings

class SendHTMLMessageToTelegram(
    private val mappings: Mappings,
    private val telegramApi: TelegramApi
) {
    suspend operator fun invoke(
        chatId: Long,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        telegramApi.sendMessage(
            chatId,
            msg,
            "HTML",
            markup = markup
        )
    }

    suspend operator fun invoke(
        monoAccountId: MonoAccountId,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        this.invoke(
            mappings.getTelegramChatIdAccByMono(monoAccountId) ?: return,
            msg,
            markup
        )
    }
}
