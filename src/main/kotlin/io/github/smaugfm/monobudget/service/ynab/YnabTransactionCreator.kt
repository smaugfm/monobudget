package io.github.smaugfm.monobudget.service.ynab

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.ynab.YnabCleared
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.service.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.util.SimpleCache
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class YnabTransactionCreator(
    private val ynab: YnabApi,
    private val monoAccountsService: MonoAccountsService,
    private val statementTransformer: MonoStatementToYnabTransactionTransformer
) {
    private val transferPayeeIdsCache = SimpleCache<String, String> {
        ynab.getAccount(it).transferPayeeId
    }

    suspend fun create(maybeTransfer: MaybeTransfer): YnabTransactionDetail = when (maybeTransfer) {
        is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.webhookResponse, maybeTransfer.processed())
        is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::process)
    }

    private suspend fun processTransfer(
        new: MonoWebhookResponseData,
        existing: YnabTransactionDetail
    ): YnabTransactionDetail {
        logger.debug { "Processing transfer transaction: $new. Existing YnabTransactionDetail: $existing" }

        val transferPayeeId =
            transferPayeeIdsCache.get(monoAccountsService.getYnabAccByMono(new.account)!!)

        val existingUpdated = ynab
            .updateTransaction(
                existing.id,
                existing
                    .toSaveTransaction()
                    .copy(payeeId = transferPayeeId, memo = "Переказ між рахунками")
            )

        val transfer = ynab.getTransaction(existingUpdated.transferTransactionId!!)

        return ynab.updateTransaction(
            transfer.id,
            transfer.toSaveTransaction().copy(cleared = YnabCleared.Cleared)
        )
    }

    private suspend fun process(webhookResponse: MonoWebhookResponseData): YnabTransactionDetail {
        logger.debug { "Processing transaction: $webhookResponse" }

        val ynabTransaction = statementTransformer(webhookResponse)

        return ynab.createTransaction(ynabTransaction)
    }
}
