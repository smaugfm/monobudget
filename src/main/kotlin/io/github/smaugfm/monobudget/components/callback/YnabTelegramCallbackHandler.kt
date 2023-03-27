package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.types.Message
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractDescriptionFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import org.koin.core.component.inject

class YnabTelegramCallbackHandler : TelegramCallbackHandler<YnabTransactionDetail>() {
    private val api: YnabApi by inject()

    override suspend fun updateTransaction(callbackType: TransactionUpdateType): YnabTransactionDetail {
        val transactionDetail = api.getTransaction(callbackType.transactionId)
        val saveTransaction = transactionDetail.toSaveTransaction()

        val newTransaction = when (callbackType) {
            is TransactionUpdateType.Uncategorize ->
                saveTransaction.copy(categoryId = null, payeeName = null, payeeId = null)

            is TransactionUpdateType.Unapprove ->
                saveTransaction.copy(approved = false)

            is TransactionUpdateType.MakePayee -> saveTransaction.copy(
                payeeId = null,
                payeeName = callbackType.payee
            )

            is TransactionUpdateType.UpdateCategory -> saveTransaction.copy(
                categoryId = callbackType.categoryId
            )
        }

        return api.updateTransaction(transactionDetail.id, newTransaction)
    }

    override suspend fun updateHTMLStatementMessage(
        updatedTransaction: YnabTransactionDetail,
        oldMessage: Message
    ): String {
        val description = extractDescriptionFromOldMessage(oldMessage)
        val (mcc, currency, transactionId) = extractFromOldMessage(oldMessage)

        return formatHTMLStatementMessage(
            "YNAB",
            description,
            mcc,
            currency,
            updatedTransaction.categoryName ?: "",
            updatedTransaction.payeeName ?: "",
            transactionId
        )
    }
}
