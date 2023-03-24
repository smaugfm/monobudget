package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.types.Message
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractDescriptionFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail

class YnabTelegramCallbackHandler(
    telegram: TelegramApi,
    private val api: YnabApi,
    formatter: TransactionMessageFormatter<YnabTransactionDetail>,
    telegramChatIds: List<Long>
) : TelegramCallbackHandler<YnabTransactionDetail>(telegram, formatter, telegramChatIds) {

    override suspend fun updateTransaction(type: TransactionUpdateType): YnabTransactionDetail {
        val transactionDetail = api.getTransaction(type.transactionId)
        val saveTransaction = transactionDetail.toSaveTransaction()

        val newTransaction = when (type) {
            is TransactionUpdateType.Uncategorize ->
                saveTransaction.copy(categoryId = null, payeeName = null, payeeId = null)

            is TransactionUpdateType.Unapprove ->
                saveTransaction.copy(approved = false)

            is TransactionUpdateType.MakePayee -> saveTransaction.copy(payeeId = null, payeeName = type.payee)
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