package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.util.ExpiryContainer
import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.ynab.YnabSaveTransaction
import com.github.smaugfm.ynab.YnabTransactionDetail
import kotlin.time.minutes
import kotlin.time.toJavaDuration

private data class NotCreated(val statement: MonoStatementItem, val transaction: YnabSaveTransaction)
private data class Created(val statement: MonoStatementItem, val transaction: YnabTransactionDetail)

class CreateTransactionHandler(
    private val ynab: YnabApi,
    private val mappings: Mappings,
) : Handler() {
    private val converter = MonoWebhookResponseToYnabTransactionConverter(mappings) {
        ynab.getPayees()
    }
    private val recentTransactions = ExpiryContainer<Created>(1.minutes.toJavaDuration())

    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@CreateTransactionHandler::handle)
    }

    private suspend fun handle(
        dispatcher: IEventDispatcher,
        e: Event.Mono.NewStatementReceived,
    ) {
        val saveTransaction = converter(e.data)
        val target = NotCreated(e.data.statementItem, saveTransaction)

        val transfer = recentTransactions.consumeCollection {
            firstOrNull {
                checkIsTransferTransactions(
                    target,
                    it
                )
            }
        }

        if (transfer != null) {
            createTransferTransaction(saveTransaction, transfer.transaction)
        } else {
            createSimpleTransaction(dispatcher, e.data, saveTransaction)
        }
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun checkIsTransferTransactions(a: NotCreated, b: Created): Boolean {
        return false
    }

    private fun createTransferTransaction(toCreate: YnabSaveTransaction, transferTo: YnabTransactionDetail) {
        // do nothing
    }

    private suspend fun createSimpleTransaction(
        dispatcher: IEventDispatcher,
        webhookResponse: MonoWebHookResponseData,
        newTransaction: YnabSaveTransaction,
    ) {
        val transactionDetail = ynab.createTransaction(newTransaction)
        dispatcher(
            Event.Telegram.SendStatementMessage(
                webhookResponse,
                transactionDetail,
            )
        )
    }
}
