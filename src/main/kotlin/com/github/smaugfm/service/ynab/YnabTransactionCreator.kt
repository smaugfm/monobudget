package com.github.smaugfm.service.ynab

import com.github.smaugfm.api.YnabApi
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.models.ynab.YnabCleared
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.service.MonoTransferBetweenAccountsDetector.MaybeTransfer
import com.github.smaugfm.service.mono.MonoAccountsService
import com.github.smaugfm.util.SimpleCache
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class YnabTransactionCreator(
    private val ynab: YnabApi,
    private val monoAccountsService: MonoAccountsService,
    private val statementTransformer: MonoStatementToYnabTransactionTransformer,
) {
    private val transferPayeeIdsCache = SimpleCache<String, String> {
        ynab.getAccount(it).transferPayeeId
    }

    suspend fun create(maybeTransfer: MaybeTransfer<YnabTransactionDetail>): YnabTransactionDetail =
        when (maybeTransfer) {
            is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.webhookResponse, maybeTransfer.processed)
            is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::process)
        }

    private suspend fun processTransfer(
        new: MonoWebhookResponseData,
        existing: YnabTransactionDetail,
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
            transfer.toSaveTransaction().copy(cleared = YnabCleared.cleared)
        )
    }

    private suspend fun process(webhookResponse: MonoWebhookResponseData): YnabTransactionDetail {
        logger.debug { "Processing transaction: $webhookResponse" }

        val ynabTransaction = statementTransformer(webhookResponse)

        return ynab.createTransaction(ynabTransaction)
    }
}
