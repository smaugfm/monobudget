package com.github.smaugfm.handlers

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.ynab.YnabSaveTransaction
import io.michaelrocks.bimap.BiMap
import java.util.logging.Logger

class MonoHandler(
    private val monoAcc2Ynab: BiMap<String, String>,
    private val monoAcc2Telegram: Map<String, Long>,
) : EventHandlerBase() {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        if (e !is Event.Mono.NewStatementReceived)
            return false

        val ynabAccountId = monoAcc2Ynab[e.data.account]
        if (ynabAccountId == null) {
            Logger.getLogger(MonoHandler::class.simpleName)
                .severe("Could not find YNAB account for Mono account ${e.data.account}")
            return true
        }
        val telegramChatId = monoAcc2Telegram[e.data.account]
        if (telegramChatId == null) {
            Logger.getLogger(MonoHandler::class.simpleName)
                .severe("Could not find Telegram chatId for Mono account ${e.data.account}")
            return true
        }

        val (transaction, categoryName) = determineTransactionParams(e.data.statementItem)

        dispatch(Event.Ynab.CreateTransaction(transaction))
        dispatch(Event.Telegram.SendStatementMessage(telegramChatId,
            e.data.statementItem,
            transaction,
            categoryName))

        return true
    }

    private fun determineTransactionParams(data: MonoStatementItem): Pair<YnabSaveTransaction, String> {
        TODO()
    }
}
