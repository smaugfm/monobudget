package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.types.Message
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractDescriptionFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import kotlinx.coroutines.reactor.awaitSingle

class LunchmoneyTelegramCallbackHandler(
    telegram: TelegramApi,
    private val api: LunchmoneyApi,
    formatter: TransactionMessageFormatter<LunchmoneyTransaction>,
    private val categorySuggestingService: CategorySuggestionService,
    telegramChatIds: List<Long>
) : TelegramCallbackHandler<LunchmoneyTransaction>(telegram, formatter, telegramChatIds) {
    override suspend fun updateTransaction(type: TransactionUpdateType): LunchmoneyTransaction {
        val txId = type.transactionId.toLong()

        val updateTransaction = when (type) {
            is TransactionUpdateType.MakePayee -> error("Not supported for Lunchmoney")
            is TransactionUpdateType.Unapprove -> LunchmoneyUpdateTransaction(
                status = LunchmoneyTransactionStatus.UNCLEARED
            )

            is TransactionUpdateType.Uncategorize -> LunchmoneyUpdateTransaction(categoryId = null)
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

        return formatHTMLStatementMessage(
            "Lunchmoney",
            description,
            mcc,
            currency,
            categorySuggestingService.categoryNameById(updatedTransaction.categoryId?.toString()) ?: "",
            updatedTransaction.payee,
            transactionId
        )
    }
}
