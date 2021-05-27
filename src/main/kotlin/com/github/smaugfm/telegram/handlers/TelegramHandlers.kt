package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.CompositeHandler
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi

class TelegramHandlers(
    telegram: TelegramApi,
    val mappings: Mappings,
) : CompositeHandler(
    listOf(
        SendStatementMessageHandler(mappings),
        CallbackQueryHandler(telegram, mappings),
        SendHTMLMessageHandler(telegram, mappings),
    ),
) {
    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
    }
}
