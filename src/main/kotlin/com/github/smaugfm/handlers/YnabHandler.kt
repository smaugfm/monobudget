package com.github.smaugfm.handlers

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.ynab.YnabCleared
import com.github.smaugfm.ynab.YnabFlagColor
import com.github.smaugfm.ynab.YnabSaveTransaction

class YnabHandler(
    private val ynab: YnabApi,
    mappings: Mappings,
) : EventHandlerBase(mappings) {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        when (e) {
            is Event.Mono.NewStatementReceived -> createTransaction(dispatch, e)
            is Event.Ynab.UpdateTransaction -> updateTransaction(dispatch, e)
            else -> return false
        }

        return true
    }

    private suspend fun updateTransaction(dispatch: Dispatch, e: Event.Ynab.UpdateTransaction) {
        val transaction = ynab.getTransaction(e.transactionId)

        val newTransactin = when (e.type) {
            TelegramHandler.Companion.UpdateType.Unclear -> transaction.copy(cleared = YnabCleared.Uncleared)
            TelegramHandler.Companion.UpdateType.MarkRed -> transaction.copy(flag_color = YnabFlagColor.Red)
            TelegramHandler.Companion.UpdateType.Unrecognized -> transaction.copy(category_id = null)
        }
    }

    private suspend fun createTransaction(dispatch: Dispatch, e: Event.Mono.NewStatementReceived) {
        val ynabAccountId = mappings.getYnabAccByMono(e.data.account) ?: return
        val telegramChatId = mappings.getTelegramChaIdAccByMono(e.data.account) ?: return

        val saveTransaction = determineTransactionParams(e.data.statementItem)

        val transactionDetail = ynab.createTransaction(saveTransaction)

        dispatch(Event.Telegram.SendStatementMessage(
            e.data,
            transactionDetail,
        ))
    }

    private fun determineTransactionParams(statementItem: MonoStatementItem): YnabSaveTransaction {
        TODO()
    }
}