package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.types.Message
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyGetSingleTransactionRequest
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyUpdateTransactionRequest
import io.github.smaugfm.lunchmoney.request.transaction.params.LunchmoneyGetSingleTransactionParams
import io.github.smaugfm.lunchmoney.request.transaction.params.LunchmoneyUpdateTransactionParams
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.extractFromOldMessage
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.util.toInsertOrUpdateTransaction
import kotlinx.coroutines.reactor.awaitSingle

class LunchmoneyTelegramCallbackHandler(
    telegram: TelegramApi,
    private val api: LunchmoneyApi,
    private val categorySuggestingService: CategorySuggestionService,
    telegramChatIds: List<Long>
) : TelegramCallbackHandler<LunchmoneyTransaction>(telegram, telegramChatIds) {
    override suspend fun updateTransaction(type: TransactionUpdateType): LunchmoneyTransaction {
        val txId = type.transactionId.toLong()
        val transaction = readTransaction(txId)
            .toInsertOrUpdateTransaction()

        val newTransaction: LunchmoneyInsertOrUpdateTransaction = when (type) {
            is TransactionUpdateType.MakePayee -> error("Not supported for Lunchmoney")
            is TransactionUpdateType.Unapprove -> transaction.copy(status = LunchmoneyTransactionStatus.UNCLEARED)
            is TransactionUpdateType.Uncategorize -> transaction.copy(categoryId = null, payee = null)
        }

        api.execute(
            LunchmoneyUpdateTransactionRequest(
                txId,
                LunchmoneyUpdateTransactionParams(
                    transaction = newTransaction,
                    debitAsNegative = true,
                    skipBalanceUpdate = false
                )
            )
        ).awaitSingle()

        return readTransaction(txId)
    }

    private suspend fun readTransaction(txId: Long) = api.execute(
        LunchmoneyGetSingleTransactionRequest(
            txId,
            LunchmoneyGetSingleTransactionParams(true)
        )
    ).awaitSingle()

    override suspend fun updateHTMLStatementMessage(
        updatedTransaction: LunchmoneyTransaction,
        oldMessage: Message
    ): String {
        val (description, mcc, currencyText, id) = extractFromOldMessage(oldMessage)

        return formatHTMLStatementMessage(
            "Lunchmoney",
            description,
            mcc,
            currencyText,
            categorySuggestingService.categoryNameById(updatedTransaction.categoryId?.toString()) ?: "",
            updatedTransaction.payee,
            id
        )
    }
}
