package com.github.smaugfm

import com.github.smaugfm.events.ExternalEvent
import com.github.smaugfm.events.IEventContext

object Handlers {
    fun newStatementReceivedHandler(e: ExternalEvent, context: IEventContext): Boolean {
        if (e !is ExternalEvent.Mono.NewStatementReceived)
            return false

        TODO()
        //context.ynab.createTransaction()
        //context.telegram.sendMessage()
        //return true
    }

    fun telegramEventReceived(e: ExternalEvent, context: IEventContext): Boolean {
        if (e !is ExternalEvent.Telegram)
            return false

        when (e) {
            is ExternalEvent.Telegram.UnclearTransaction -> TODO()//context.ynab.markTransactionUnclered()
            is ExternalEvent.Telegram.MarkTransactionRed -> TODO()//context.ynab.markTransactionRed()
        }

        //return true
    }
}