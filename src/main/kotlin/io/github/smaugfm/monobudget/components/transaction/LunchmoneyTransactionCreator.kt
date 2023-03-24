package io.github.smaugfm.monobudget.components.transaction

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyCreateTransactionGroupRequest
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyGetSingleTransactionRequest
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyInsertTransactionsRequest
import io.github.smaugfm.lunchmoney.request.transaction.LunchmoneyUpdateTransactionRequest
import io.github.smaugfm.lunchmoney.request.transaction.params.LunchmoneyCreateTransactionGroupParams
import io.github.smaugfm.lunchmoney.request.transaction.params.LunchmoneyInsertTransactionRequestParams
import io.github.smaugfm.lunchmoney.request.transaction.params.LunchmoneyUpdateTransactionParams
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.components.transaction.factory.NewTransactionFactory
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class LunchmoneyTransactionCreator(
    private val api: LunchmoneyApi,
    private val transferCategoryId: Long,
    newTransactionFactory: NewTransactionFactory<LunchmoneyInsertTransaction>
) : BudgetTransactionCreator<LunchmoneyTransaction, LunchmoneyInsertTransaction>(newTransactionFactory) {

    override suspend fun create(maybeTransfer: MaybeTransfer<LunchmoneyTransaction>) = when (maybeTransfer) {
        is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.webhookResponse, maybeTransfer.processed())
        is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::processSingle)
    }

    private suspend fun processTransfer(
        newWebhookResponse: MonoWebhookResponseData,
        existingTransaction: LunchmoneyTransaction
    ): LunchmoneyTransaction {
        log.debug {
            "Processed transfer transaction: $newWebhookResponse. " +
                "Existing LunchmoneyTransaction: $existingTransaction"
        }

        api.execute(
            LunchmoneyUpdateTransactionRequest(
                existingTransaction.id,
                LunchmoneyUpdateTransactionParams(
                    LunchmoneyUpdateTransaction(
                        status = LunchmoneyTransactionStatus.CLEARED,
                        categoryId = transferCategoryId
                    )
                )
            )
        ).awaitSingle()
        val newTransaction = processSingle(newWebhookResponse, true)

        val groupId = api.execute(
            LunchmoneyCreateTransactionGroupRequest(
                LunchmoneyCreateTransactionGroupParams(
                    date = newTransaction.date,
                    payee = TRANSFER_PAYEE,
                    transactions = listOf(existingTransaction, newTransaction).map { it.id },
                    categoryId = transferCategoryId
                )
            )
        ).awaitSingle()

        log.debug { "Created new Lunchmoney transaction group id=$groupId" }

        return newTransaction
    }

    private suspend fun processSingle(
        webhookResponse: MonoWebhookResponseData,
        partOfTransfer: Boolean = false
    ): LunchmoneyTransaction {
        log.debug { "Processing transaction: $webhookResponse" }

        val newTransaction =
            newTransactionFactory.create(webhookResponse).let {
                if (partOfTransfer) {
                    it.copy(
                        status = LunchmoneyTransactionStatus.CLEARED,
                        categoryId = transferCategoryId
                    )
                } else {
                    it
                }
            }
        val createdId = api.execute(
            LunchmoneyInsertTransactionsRequest(
                LunchmoneyInsertTransactionRequestParams(
                    transactions = listOf(newTransaction),
                    applyRules = true,
                    checkForRecurring = true,
                    debitAsNegative = true
                )
            )
        ).awaitSingle().ids.first()
        return api.execute(LunchmoneyGetSingleTransactionRequest(createdId)).awaitSingle()
    }

    companion object {
        private const val TRANSFER_PAYEE = "Transfer"
    }
}