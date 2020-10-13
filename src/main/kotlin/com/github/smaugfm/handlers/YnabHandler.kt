package com.github.smaugfm.handlers

import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.ynab.*

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

    suspend fun updateTransaction(dispatch: Dispatch, e: Event.Ynab.UpdateTransaction) {
        val transactionDetail = ynab.getTransaction(e.transactionId)
        val saveTransaction = ynabTransactionSaveFromDetails(transactionDetail)

        val newTransaction = when (e.type) {
            TelegramHandler.Companion.UpdateType.Unclear -> saveTransaction.copy(cleared = YnabCleared.Uncleared)
            TelegramHandler.Companion.UpdateType.MarkRed -> saveTransaction.copy(flag_color = YnabFlagColor.Red)
            TelegramHandler.Companion.UpdateType.Unrecognized -> saveTransaction.copy(category_id = null)
        }

        ynab.updateTransaction(transactionDetail.id, newTransaction)
    }

    private suspend fun createTransaction(dispatch: Dispatch, e: Event.Mono.NewStatementReceived) {
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

    companion object {
        fun ynabTransactionSaveFromDetails(t: YnabTransactionDetail): YnabSaveTransaction {
            return YnabSaveTransaction(t.account_id,
                t.date,
                t.amount,
                t.payee_id,
                null,
                t.category_id,
                t.memo,
                t.cleared,
                t.approved,
                t.flag_color,
                t.import_id,
                t.subtransactions.map {
                    YnabSaveSubTransaction(it.amount,
                        it.payee_id,
                        it.payee_name,
                        it.category_id,
                        it.memo)
                }
            )
        }
    }
}