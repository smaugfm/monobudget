package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.misc.SimpleCache
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.mono.TransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.ynab.model.YnabCleared
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class YnabTransactionFactory(
    private val api: YnabApi,
    private val accounts: AccountsService
) : TransactionFactory<YnabTransactionDetail, YnabSaveTransaction>() {

    private val transferPayeeIdsCache = SimpleCache<String, String> {
        api.getAccount(it).transferPayeeId
    }

    override suspend fun create(maybeTransfer: MaybeTransfer<YnabTransactionDetail>) = when (maybeTransfer) {
        is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.statement, maybeTransfer.processed())
        is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::processSingle)
    }

    private suspend fun processTransfer(
        statement: StatementItem,
        existingTransaction: YnabTransactionDetail
    ): YnabTransactionDetail {
        log.debug {
            "Processing transfer transaction: $statement. " +
                "Existing YnabTransactionDetail: $existingTransaction"
        }

        val transferPayeeId =
            transferPayeeIdsCache.get(accounts.getBudgetAccountId(statement.accountId)!!)

        val existingTransactionUpdated = api
            .updateTransaction(
                existingTransaction.id,
                existingTransaction
                    .toSaveTransaction()
                    .copy(payeeId = transferPayeeId, memo = "Переказ між рахунками")
            )

        val transfer = api.getTransaction(existingTransactionUpdated.transferTransactionId!!)

        return api.updateTransaction(
            transfer.id,
            transfer.toSaveTransaction().copy(cleared = YnabCleared.Cleared)
        )
    }

    private suspend fun processSingle(statement: StatementItem): YnabTransactionDetail {
        log.debug { "Processing transaction: $statement" }

        val transaction = newTransactionFactory.create(statement)

        return api.createTransaction(transaction)
    }
}
