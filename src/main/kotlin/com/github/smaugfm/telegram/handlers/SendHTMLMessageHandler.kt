package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi

class SendHTMLMessageHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : Handler() {

    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@SendHTMLMessageHandler::handle)
    }

    private suspend fun handle(
        event: Event.Telegram.SendHTMLMessage,
    ) {
        val (chatId, msg, markup) = event

        telegram.sendMessage(
            chatId,
            msg,
            "HTML",
            markup = markup
        )
    }
}
