package io.github.smaugfm.monobudget.service.transaction

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.ynab.YnabCleared
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.service.statement.MonoStatementToYnabTransactionTransformer
import io.github.smaugfm.monobudget.util.SimpleCache
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class YnabTransactionCreator(
    private val api: YnabApi,
    private val monoAccountsService: MonoAccountsService,
    private val statementTransformer: MonoStatementToYnabTransactionTransformer
) : TransactionCreator<YnabTransactionDetail>() {
    private val transferPayeeIdsCache = SimpleCache<String, String> {
        api.getAccount(it).transferPayeeId
    }

    override suspend fun create(maybeTransfer: MaybeTransfer<YnabTransactionDetail>) = when (maybeTransfer) {
        is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.webhookResponse, maybeTransfer.processed())
        is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::processSingle)
    }

    private suspend fun processTransfer(
        newWebhookResponse: MonoWebhookResponseData,
        existingTransaction: YnabTransactionDetail
    ): YnabTransactionDetail {
        logger.debug {
            "Processing transfer transaction: $newWebhookResponse. " +
                "Existing YnabTransactionDetail: $existingTransaction"
        }

        val transferPayeeId =
            transferPayeeIdsCache.get(monoAccountsService.getBudgetAccountId(newWebhookResponse.account)!!)

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

    private suspend fun processSingle(webhookResponse: MonoWebhookResponseData): YnabTransactionDetail {
        logger.debug { "Processing transaction: $webhookResponse" }

        val transaction = statementTransformer.transform(webhookResponse)

        return api.createTransaction(transaction)
    }
}
