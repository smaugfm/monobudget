package io.github.smaugfm.monobudget.service.lunchmoney

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.service.MonoTransferBetweenAccountsDetector.MaybeTransfer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class LunchmoneyTransactionCreator(
    private val api: LunchmoneyApi
) {
    suspend fun create(maybeTransfer: MaybeTransfer): Any = when (maybeTransfer) {
        is MaybeTransfer.Transfer -> processTransfer(maybeTransfer.webhookResponse, maybeTransfer.processed())
        is MaybeTransfer.NotTransfer -> maybeTransfer.consume(::process)
    }

    private suspend fun processTransfer(new: MonoWebhookResponseData, existing: Any): Any {
        TODO()
    }

    private suspend fun process(webhookResponse: MonoWebhookResponseData): Any {
        logger.debug { "Processing transaction: $webhookResponse" }

        // val ynabTransaction = statementTransformer(webhookResponse)
        //
        // return api.execute(ynabTransaction)
        TODO()
    }
}
