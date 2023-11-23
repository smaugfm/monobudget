package io.github.smaugfm.monobudget.ynab

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.MaybeTransferStatement
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.ynab.model.YnabCleared
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Single
class YnabTransactionFactory(
    private val api: YnabApi,
    private val bankAccounts: BankAccountService,
) : TransactionFactory<YnabTransactionDetail, YnabSaveTransaction>() {
    private val transferPayeeIdsCache = ConcurrentHashMap<String, String>()

    override suspend fun create(maybeTransfer: MaybeTransferStatement<YnabTransactionDetail>) =
        when (maybeTransfer) {
            is MaybeTransferStatement.Transfer ->
                processTransfer(
                    maybeTransfer.statement,
                    maybeTransfer.processed(),
                )
            is MaybeTransferStatement.NotTransfer -> maybeTransfer.consume(::processSingle)
        }

    private suspend fun processTransfer(
        statement: StatementItem,
        existingTransaction: YnabTransactionDetail,
    ): YnabTransactionDetail {
        log.debug {
            "Processing transfer transaction: $statement. " +
                "Existing YnabTransactionDetail: $existingTransaction"
        }

        val transferPayeeId =
            transferPayeeIdsCache.getOrPut(bankAccounts.getBudgetAccountId(statement.accountId)!!) {
                api.getAccount(statement.accountId).transferPayeeId
            }

        val existingTransactionUpdated =
            api
                .updateTransaction(
                    existingTransaction.id,
                    existingTransaction
                        .toSaveTransaction()
                        .copy(payeeId = transferPayeeId, memo = "Переказ між рахунками"),
                )

        val transfer = api.getTransaction(existingTransactionUpdated.transferTransactionId!!)

        return api.updateTransaction(
            transfer.id,
            transfer.toSaveTransaction().copy(cleared = YnabCleared.Cleared),
        )
    }

    private suspend fun processSingle(statement: StatementItem): YnabTransactionDetail {
        log.debug { "Processing transaction: $statement" }

        val transaction = newTransactionFactory.create(statement)

        return api.createTransaction(transaction)
    }
}
