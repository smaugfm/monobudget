package io.github.smaugfm.monobudget.ynab

import com.elbekd.bot.types.Message
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.notify.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Single

@Single
class YnabTelegramCallbackHandler(
    private val api: YnabApi,
) : TelegramCallbackHandler<YnabTransactionDetail>() {
    override suspend fun updateTransaction(callbackType: TransactionUpdateType): YnabTransactionDetail {
        val transactionDetail = api.getTransaction(callbackType.transactionId)
        val saveTransaction = transactionDetail.toSaveTransaction()

        val newTransaction =
            when (callbackType) {
                is TransactionUpdateType.Uncategorize ->
                    saveTransaction.copy(categoryId = null, payeeName = null, payeeId = null)

                is TransactionUpdateType.Unapprove ->
                    saveTransaction.copy(approved = false)

                is TransactionUpdateType.MakePayee ->
                    saveTransaction.copy(
                        payeeId = null,
                        payeeName = callbackType.payee,
                    )

                is TransactionUpdateType.UpdateCategory ->
                    saveTransaction.copy(
                        categoryId = callbackType.categoryId,
                    )
            }

        return api.updateTransaction(transactionDetail.id, newTransaction)
    }

    override suspend fun updateHTMLStatementMessage(
        updatedTransaction: YnabTransactionDetail,
        oldMessage: Message,
    ): String {
        val (description, mcc, currency) = extractFromOldMessage(oldMessage)

        val category = categoryService.budgetedCategoryById(updatedTransaction.categoryId)
        return formatHTMLStatementMessage(
            "YNAB",
            description,
            mcc,
            currency,
            category,
            updatedTransaction.payeeName ?: "",
            updatedTransaction.id,
        )
    }
}
