package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.settings.Mappings
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SendStatementMessageHandler(
    val mappings: Mappings,
) : Handler() {
    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@SendStatementMessageHandler::handle)
    }

    suspend fun handle(
        dispatcher: IEventDispatcher,
        event: Event.Telegram.SendStatementMessage,
    ) {
        val monoResponse = event.mono
        val accountCurrency = mappings.getAccountCurrency(event.mono.account)!!
        val transaction = event.transaction
        val telegramChatId = mappings.getTelegramChatIdAccByMono(monoResponse.account) ?: return

        dispatcher(
            Event.Telegram.SendHTMLMessage(
                telegramChatId,
                formatHTMLStatementMessage(accountCurrency, monoResponse.statementItem, transaction),
                formatInlineKeyboard(emptySet()),
            )
        )
    }
}
