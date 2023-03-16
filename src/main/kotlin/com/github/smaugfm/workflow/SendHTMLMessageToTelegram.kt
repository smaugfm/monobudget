package com.github.smaugfm.workflow

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.models.settings.Mappings

class SendHTMLMessageToTelegram(
    private val mappings: Mappings,
    private val telegramApi: TelegramApi
) {
    suspend operator fun invoke(
        chatId: ChatId,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        telegramApi.sendMessage(
            chatId,
            msg,
            ParseMode.Html,
            markup
        )
    }

    suspend operator fun invoke(
        monoAccountId: String,
        msg: String,
        markup: ReplyKeyboard? = null,
    ) {
        this.invoke(
            mappings.getTelegramChatIdAccByMono(monoAccountId)?.let(ChatId::IntegerId) ?: return,
            msg,
            markup
        )
    }
}
