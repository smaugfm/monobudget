package io.github.smaugfm.monobudget.lunchmoney

import com.elbekd.bot.types.Message
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.extractDescriptionFromOldMessage
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyTransactionMessageFormatter.Companion.constructTransactionsQuickUrl
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.datetime.toKotlinLocalDate
import org.koin.core.annotation.Single

@Single
class LunchmoneyTelegramCallbackHandler(
    private val api: LunchmoneyApi
) : TelegramCallbackHandler<LunchmoneyTransaction>() {

    override suspend fun updateTransaction(callbackType: TransactionUpdateType): LunchmoneyTransaction {
        val txId = callbackType.transactionId.toLong()

        val updateTransaction = when (callbackType) {
            is TransactionUpdateType.MakePayee -> error("Not supported for Lunchmoney")
            is TransactionUpdateType.Unapprove -> LunchmoneyUpdateTransaction(
                status = LunchmoneyTransactionStatus.UNCLEARED
            )

            is TransactionUpdateType.Uncategorize ->
                LunchmoneyUpdateTransaction(categoryId = null)

            is TransactionUpdateType.UpdateCategory -> LunchmoneyUpdateTransaction(
                categoryId = callbackType.categoryId.toLong(),
                status = LunchmoneyTransactionStatus.CLEARED
            )
        }

        api.updateTransaction(
            transactionId = txId,
            transaction = updateTransaction,
            debitAsNegative = true,
            skipBalanceUpdate = false
        ).awaitSingle()

        return readTransaction(txId)
    }

    private suspend fun readTransaction(txId: Long) = api
        .getSingleTransaction(txId, debitAsNegative = true)
        .awaitSingle()

    override suspend fun updateHTMLStatementMessage(
        updatedTransaction: LunchmoneyTransaction,
        oldMessage: Message
    ): String {
        val description = extractDescriptionFromOldMessage(oldMessage)
        val (mcc, currency, transactionId) = extractFromOldMessage(oldMessage)
        val category = categoryService.budgetedCategoryById(updatedTransaction.categoryId?.toString())

        return formatHTMLStatementMessage(
            "Lunchmoney",
            description,
            mcc,
            currency,
            category,
            updatedTransaction.payee,
            transactionId,
            constructTransactionsQuickUrl(updatedTransaction.date.toKotlinLocalDate())
        )
    }
}
