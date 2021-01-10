package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TransactionUpdateType
import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.ynab.YnabTransactionDetail

class UpdateTransactionHandler(
    private val ynab: YnabApi,
    val mappings: Mappings,
) : Handler() {
    override fun HandlersBuilder.registerHandlerFunctions() {
        register(this@UpdateTransactionHandler::handle)
    }

    suspend fun handle(
        e: Event.Ynab.UpdateTransaction,
    ): YnabTransactionDetail {
        val transactionDetail = ynab.getTransaction(e.type.transactionId)
        val saveTransaction = transactionDetail.toSaveTransaction()

        val newTransaction = when (e.type) {
            is TransactionUpdateType.Uncategorize ->
                saveTransaction.copy(category_id = null, payee_name = null, payee_id = null)
            is TransactionUpdateType.Unapprove ->
                saveTransaction.copy(approved = false)
            is TransactionUpdateType.Unknown -> saveTransaction.copy(
                payee_id = mappings.unknownPayeeId,
                category_id = mappings.unknownCategoryId,
                payee_name = null
            )
            is TransactionUpdateType.MakePayee -> saveTransaction.copy(payee_id = null, payee_name = e.type.payee)
        }

        return ynab.updateTransaction(transactionDetail.id, newTransaction)
    }
}
