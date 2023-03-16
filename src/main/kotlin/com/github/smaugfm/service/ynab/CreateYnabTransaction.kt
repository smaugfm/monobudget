package com.github.smaugfm.service.ynab

import com.github.smaugfm.api.YnabApi
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.models.ynab.YnabCleared
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.util.ExpiringMap
import com.github.smaugfm.util.SimpleCache
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class CreateYnabTransaction(
    private val ynab: YnabApi,
    private val mappings: Mappings,
    private val transform: TransformStatementToYnabTransaction,
) {
    private val transferPayeeIdsCache = SimpleCache<String, String> {
        ynab.getAccount(it).transferPayeeId
    }
    private val webhookResponsesCache = SimpleCache<MonoWebhookResponseData, Unit> {}
    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<YnabTransactionDetail>>(1.minutes)

    suspend operator fun invoke(webhookResponseData: MonoWebhookResponseData): YnabTransactionDetail? {
        logIncoming(mappings, webhookResponseData)

        if (!webhookResponsesCache.alreadyHasKey(webhookResponseData, Unit)) {
            logger.info { "Duplicate ${MonoWebhookResponseData::class.simpleName} $webhookResponseData" }
            return null
        }

        val existingTransfer = recentTransactions.entries.firstOrNull { (recentStatementItem) ->
            checkIsTransferTransactions(
                webhookResponseData.statementItem,
                recentStatementItem
            ).also {
                if (it) {
                    logger.debug { "Found transfer. Waiting for deferred to finish." }
                }
            }
        }?.value?.await()

        return process(existingTransfer, webhookResponseData)
    }

    private suspend fun process(
        existingTransfer: YnabTransactionDetail?,
        webhookResponseData: MonoWebhookResponseData
    ): YnabTransactionDetail {
        val newTransaction = if (existingTransfer != null) {
            processTransfer(webhookResponseData, existingTransfer)
        } else {
            val transactionDetailDeferred = CompletableDeferred<YnabTransactionDetail>()
            recentTransactions.add(webhookResponseData.statementItem, transactionDetailDeferred)

            processSimple(webhookResponseData).also {
                transactionDetailDeferred.complete(it)
            }
        }
        return newTransaction
    }

    private suspend fun processTransfer(
        new: MonoWebhookResponseData,
        existing: YnabTransactionDetail,
    ): YnabTransactionDetail {
        logger.debug {
            "Found matching transfer transaction.\n" +
                "Current: $new\n" +
                "Recent transfer: $existing"
        }

        val transferPayeeId =
            transferPayeeIdsCache.get(mappings.getYnabAccByMono(new.account)!!)

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

    private suspend fun processSimple(webhookResponse: MonoWebhookResponseData): YnabTransactionDetail {
        logger.debug { "Processing simple transaction: $webhookResponse" }
        val ynabTransaction = transform(webhookResponse)

        return ynab.createTransaction(ynabTransaction)
    }

    companion object {
        private const val TRANSFER_MCC = 4829

        private fun checkIsTransferTransactions(
            new: MonoStatementItem,
            existing: MonoStatementItem
        ): Boolean {
            val amountMatch = abs(new.amount) == abs(existing.amount)
            val currencyMatch = new.currencyCode == existing.currencyCode
            val signMatch = new.amount > 0 && existing.amount < 0 || new.amount < 0 && existing.amount > 0
            val mccMatch = new.mcc == TRANSFER_MCC && existing.mcc == TRANSFER_MCC

            return amountMatch && currencyMatch && signMatch && mccMatch
        }

        private fun logIncoming(
            mappings: Mappings,
            webhookResponseData: MonoWebhookResponseData
        ) {
            with(webhookResponseData) {
                logger.info {
                    "Incoming transaction from ${mappings.getMonoAccAlias(account)}'s account.\n" +
                        with(statementItem) {
                            "\tAmount: ${amount}${currencyCode}\n" +
                                "\tMemo: $comment"
                        }
                }
            }
        }
    }
}
