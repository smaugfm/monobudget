package io.github.smaugfm.monobudget.lunchmoney

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.exception.LunchmoneyApiResponseException
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobudget.common.account.MaybeTransfer
import io.github.smaugfm.monobudget.common.exception.BudgetBackendException
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

private val log = KotlinLogging.logger {}

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class LunchmoneyTransactionCreator(
    budgetBackend: BudgetBackend.Lunchmoney,
    private val ctx: StatementProcessingContext,
    private val api: LunchmoneyApi,
) : TransactionFactory<LunchmoneyTransaction, LunchmoneyInsertTransaction>() {
    private val transferCategoryId = budgetBackend.transferCategoryId.toLong()

    override suspend fun create(maybeTransfer: MaybeTransfer<LunchmoneyTransaction>) =
        try {
            when (maybeTransfer) {
                is MaybeTransfer.Transfer ->
                    processTransfer(maybeTransfer.statement, maybeTransfer.processed())

                is MaybeTransfer.NotTransfer ->
                    maybeTransfer.consume(::processSingle)
            }
        } catch (e: LunchmoneyApiResponseException) {
            val template = "Текст помилки: "
            throw BudgetBackendException(
                e,
                template + e.message,
            )
        }

    private suspend fun processTransfer(
        statement: StatementItem,
        existingTransaction: LunchmoneyTransaction,
    ): LunchmoneyTransaction {
        log.debug {
            "Processed transfer transaction: $statement. " +
                "Existing LunchmoneyTransaction: $existingTransaction"
        }

        ctx.execIfFirst("transactionUpdated") {
            api.updateTransaction(
                transactionId = existingTransaction.id,
                transaction =
                    LunchmoneyUpdateTransaction(
                        status = LunchmoneyTransactionStatus.CLEARED,
                        categoryId = transferCategoryId,
                    ),
            )
                .awaitSingle()
        }

        val newTransaction = processSingle(statement, true)

        val groupId =
            ctx.getOrPut("transactionGroupId") {
                api.createTransactionGroup(
                    date = newTransaction.date,
                    payee = TRANSFER_PAYEE,
                    transactions = listOf(existingTransaction, newTransaction).map { it.id },
                    categoryId = transferCategoryId,
                )
                    .awaitSingle()
            }

        log.debug { "Created new Lunchmoney transaction group id=$groupId" }

        return newTransaction
    }

    private suspend fun processSingle(
        statement: StatementItem,
        partOfTransfer: Boolean = false,
    ): LunchmoneyTransaction {
        log.debug { "Processing transaction: $statement" }

        val newTransaction =
            newTransactionFactory.create(statement).let {
                if (partOfTransfer) {
                    it.copy(
                        status = LunchmoneyTransactionStatus.CLEARED,
                        categoryId = transferCategoryId,
                    )
                } else {
                    it
                }
            }

        val createdId =
            ctx.getOrPut("transactionCreatedId") {
                api.insertTransactions(
                    transactions = listOf(newTransaction),
                    applyRules = true,
                    checkForRecurring = true,
                    debitAsNegative = true,
                ).awaitSingle()
                    .first()
            }
        return ctx.getOrPut("transaction") {
            api.getSingleTransaction(createdId)
                .awaitSingle()
        }
    }

    companion object {
        private const val TRANSFER_PAYEE = "Transfer"
    }
}
