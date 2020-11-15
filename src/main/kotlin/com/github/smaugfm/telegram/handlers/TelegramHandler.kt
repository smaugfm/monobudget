package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.CompositeHandler
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi

class TelegramHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : CompositeHandler(
    listOf(
        SendStatementMessageHandler(telegram, mappings),
        CallbackQueryHandler(telegram, mappings),
        MessagesHandler()
    ),
) {
    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
    }
}
