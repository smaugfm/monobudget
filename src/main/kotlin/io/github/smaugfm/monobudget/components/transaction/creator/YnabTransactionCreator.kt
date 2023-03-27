package io.github.smaugfm.monobudget.components.transaction.creator

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.components.transaction.factory.NewTransactionFactory
import io.github.smaugfm.monobudget.model.ynab.YnabCleared
import io.github.smaugfm.monobudget.model.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.util.SimpleCache
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class YnabTransactionCreator(
    private val api: YnabApi,
    private val monoAccountsService: MonoAccountsService,
    newTransactionFactory: NewTransactionFactory<YnabSaveTransaction>
) : BudgetTransactionCreator<YnabTransactionDetail, YnabSaveTransaction>(newTransactionFactory) {
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
        log.debug {
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
        log.debug { "Processing transaction: $webhookResponse" }

        val transaction = newTransactionFactory.create(webhookResponse)

        return api.createTransaction(transaction)
    }
}
