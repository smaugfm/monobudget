package com.github.smaugfm.ynab

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.EventHandlerBase
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TransactionActionType

class YnabHandler(
    private val ynab: YnabApi,
    mappings: Mappings,
) : EventHandlerBase(YnabHandler::class.simpleName.toString(), mappings) {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        when (e) {
            is Event.Mono.NewStatementReceived -> createTransaction(dispatch, e)
            is Event.Ynab.TransactionAction -> updateTransaction(e)
            else -> return false
        }

        return true
    }

    suspend fun updateTransaction(e: Event.Ynab.TransactionAction) {
        val transactionDetail = ynab.getTransaction(e.type.transactionId)
        val saveTransaction = ynabTransactionSaveFromDetails(transactionDetail)

        val newTransaction = when (e.type) {
            is TransactionActionType.Uncategorize -> saveTransaction.copy(category_id = null)
            is TransactionActionType.Unpayee -> saveTransaction.copy(
                payee_id = null,
                payee_name = null,
                approved = false
            )
            is TransactionActionType.Unapprove -> saveTransaction.copy(approved = false)
            is TransactionActionType.Unknown -> saveTransaction.copy(
                payee_id = mappings.unknownPayeeId,
                category_id = mappings.unknownCategoryId,
                payee_name = null
            )
            is TransactionActionType.MakePayee -> saveTransaction.copy(payee_id = null, payee_name = e.type.payeeName)
        }

        ynab.updateTransaction(transactionDetail.id, newTransaction)
    }

    private suspend fun createTransaction(dispatch: Dispatch, e: Event.Mono.NewStatementReceived) {
        val saveTransaction = determineTransactionParams(e.data.statementItem)
        val transactionDetail = ynab.createTransaction(saveTransaction)

        dispatch(
            Event.Telegram.SendStatementMessage(
                e.data,
                transactionDetail,
            )
        )
    }

    private fun determineTransactionParams(statementItem: MonoStatementItem): YnabSaveTransaction {
        TODO()
    }

    companion object {
        fun ynabTransactionSaveFromDetails(t: YnabTransactionDetail): YnabSaveTransaction {
            return YnabSaveTransaction(
                t.account_id,
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
                    YnabSaveSubTransaction(
                        it.amount,
                        it.payee_id,
                        it.payee_name,
                        it.category_id,
                        it.memo
                    )
                }
            )
        }
    }
}
