package com.github.smaugfm.ynab

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.EventHandlerBase
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.util.PayeeSuggestor
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class YnabHandler(
    private val ynab: YnabApi,
    mappings: Mappings,
) : EventHandlerBase(YnabHandler::class.simpleName.toString(), mappings) {
    val payeeSuggestor = PayeeSuggestor()

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
        val saveTransaction = determineTransactionParams(e.data)
        val transactionDetail = ynab.createTransaction(saveTransaction)

        dispatch(
            Event.Telegram.SendStatementMessage(
                e.data,
                transactionDetail,
            )
        )
    }

    private fun determineTransactionParams(data: MonoWebHookResponseData): YnabSaveTransaction {
        return YnabSaveTransaction(
            account_id = mappings.getYnabAccByMono(data.account)
                ?: throw IllegalStateException("Could not find YNAB account for mono account ${data.account}"),
            date = data.statementItem.time.toLocalDateTime(TimeZone.currentSystemDefault()).date,
            amount = data.statementItem.amount,
            payee_id = TODO(),
            payee_name = TODO(),
            category_id = TODO(),
            memo = data.statementItem.description,
            cleared = YnabCleared.Cleared,
            approved = true,
            flag_color = null,
            import_id = null,
            subtransactions = emptyList()
        )
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
