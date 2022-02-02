package com.github.smaugfm.workflows

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.models.MonoStatementItem
import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.YnabCleared
import com.github.smaugfm.models.YnabTransactionDetail
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.util.ExpiringMap
import com.github.smaugfm.workflows.util.MonoWebhookResponseToYnabTransactionConverter
import com.github.smaugfm.workflows.util.UniqueWebhookResponses
import com.github.smaugfm.workflows.util.YnabTransferPayeeIdsCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class ProcessWebhookWorkflow(
    private val ynab: YnabApi,
    private val mappings: Mappings,
    private val converter: MonoWebhookResponseToYnabTransactionConverter,
    private val webhookResponsesCache: UniqueWebhookResponses,
    private val transferPayeeIdsCache: YnabTransferPayeeIdsCache,
) {
    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<YnabTransactionDetail>>(1.minutes)
    private val incomingMutex = Mutex(false)

    suspend operator fun invoke(webhookResponseData: MonoWebHookResponseData): YnabTransactionDetail? {
        if (!webhookResponsesCache.isUnique(webhookResponseData)) {
            logger.info("Duplicate ${MonoWebHookResponseData::class.simpleName} $webhookResponseData")
            return null
        }

        incomingMutex.lock()

        try {
            val transferDeferred = recentTransactions.consumeCollection {
                entries.firstOrNull {
                    checkIsTransferTransactions(
                        webhookResponseData.statementItem,
                        it.key
                    )
                }?.value
            }

            val newTransaction = if (transferDeferred != null) {
                incomingMutex.unlock()
                processTransfer(webhookResponseData, transferDeferred)
            } else {
                val transactionDetailDeferred = CompletableDeferred<YnabTransactionDetail>()
                recentTransactions.add(webhookResponseData.statementItem, transactionDetailDeferred)

                incomingMutex.unlock()
                processSimple(webhookResponseData).also {
                    transactionDetailDeferred.complete(it)
                }
            }

            return newTransaction
        } finally {
            if (incomingMutex.isLocked)
                incomingMutex.unlock()
        }
    }

    private fun checkIsTransferTransactions(new: MonoStatementItem, existing: MonoStatementItem): Boolean {
        val amountMatch = abs(new.amount) == abs(existing.amount)
        val currencyMatch = new.currencyCode == existing.currencyCode
        val signMatch = new.amount > 0 && existing.amount < 0 || new.amount < 0 && existing.amount > 0
        val mccMatch = new.mcc == TRANSFER_MCC && existing.mcc == TRANSFER_MCC

        return amountMatch && currencyMatch && signMatch && mccMatch
    }

    private suspend fun processTransfer(
        new: MonoWebHookResponseData,
        existingDeferred: Deferred<YnabTransactionDetail>,
    ): YnabTransactionDetail {
        val transferPayeeId =
            transferPayeeIdsCache.get(mappings.getYnabAccByMono(new.account)!!)

        val existing = existingDeferred.await()
        val existingUpdated = ynab
            .updateTransaction(
                existing.id,
                existing
                    .toSaveTransaction()
                    .copy(payee_id = transferPayeeId, memo = "Перечисление между счетами")
            )

        val transfer = ynab.getTransaction(existingUpdated.transfer_transaction_id!!)

        return ynab.updateTransaction(
            transfer.id,
            transfer.toSaveTransaction().copy(cleared = YnabCleared.cleared)
        )
    }

    private suspend fun processSimple(webhookResponse: MonoWebHookResponseData): YnabTransactionDetail =
        ynab.createTransaction(converter(webhookResponse))

    companion object {
        private const val TRANSFER_MCC = 4829
    }
}
