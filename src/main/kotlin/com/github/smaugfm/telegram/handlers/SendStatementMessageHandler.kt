package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class SendStatementMessageHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : Handler() {
    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@SendStatementMessageHandler::handle)
    }

    suspend fun handle(
        event: Event.Telegram.SendStatementMessage,
    ) {
        val monoResponse = event.mono
        val accountCurrency = mappings.getAccountCurrency(event.mono.account)!!
        val transaction = event.transaction
        val telegramChatId = mappings.getTelegramChatIdAccByMono(monoResponse.account) ?: return

        telegram.sendMessage(
            telegramChatId,
            formatHTMLStatementMessage(accountCurrency, monoResponse.statementItem, transaction),
            "HTML",
            markup = formatInlineKeyboard(emptySet())
        )
    }
}
