package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventsHandlerRegistrar
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi

class TelegramHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : IEventsHandlerRegistrar {
    lateinit var sendStatementMessage: SendStatementMessageHandler
    lateinit var callbackQuery: CallbackQueryHandler

    override fun registerEvents(builder: HandlersBuilder) {
        sendStatementMessage = SendStatementMessageHandler(telegram, mappings).apply { registerEvents(builder) }
        callbackQuery = CallbackQueryHandler(telegram, mappings).apply { registerEvents(builder) }
    }

    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
    }
}
