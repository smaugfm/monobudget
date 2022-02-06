package com.github.smaugfm.workflows

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.models.MonoAccountId
import com.github.smaugfm.models.MonoStatementItem
import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.YnabCleared
import com.github.smaugfm.models.YnabTransactionDetail
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.util.ExpiringMap
import com.github.smaugfm.util.SimpleCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class CreateTransaction(
    private val ynab: YnabApi,
    private val mappings: Mappings,
    private val transform: TransformStatementToYnabTransaction,
) {
    private val transferPayeeIdsCache = SimpleCache<MonoAccountId, String> {
        ynab.getAccount(it).transfer_payee_id
    }
    private val webhookResponsesCache = SimpleCache<MonoWebHookResponseData, Unit> {}
    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<YnabTransactionDetail>>(1.minutes)

    suspend operator fun invoke(webhookResponseData: MonoWebHookResponseData): YnabTransactionDetail? {
        logIncoming(mappings, webhookResponseData)

        if (!webhookResponsesCache.alreadyHasKey(webhookResponseData, Unit)) {
            logger.info { "Duplicate ${MonoWebHookResponseData::class.simpleName} $webhookResponseData" }
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
        new: MonoWebHookResponseData,
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
                    .copy(payee_id = transferPayeeId, memo = "Перечисление между счетами")
            )

        val transfer = ynab.getTransaction(existingUpdated.transfer_transaction_id!!)

        return ynab.updateTransaction(
            transfer.id,
            transfer.toSaveTransaction().copy(cleared = YnabCleared.cleared)
        )
    }

    private suspend fun processSimple(webhookResponse: MonoWebHookResponseData): YnabTransactionDetail {
        logger.debug { "Processing simple transaction: $webhookResponse" }
        val ynabTransaction = transform(webhookResponse)

        return ynab.createTransaction(ynabTransaction)
    }

    companion object {
        private const val TRANSFER_MCC = 4829

        private fun checkIsTransferTransactions(new: MonoStatementItem, existing: MonoStatementItem): Boolean {
            val amountMatch = abs(new.amount) == abs(existing.amount)
            val currencyMatch = new.currencyCode == existing.currencyCode
            val signMatch = new.amount > 0 && existing.amount < 0 || new.amount < 0 && existing.amount > 0
            val mccMatch = new.mcc == TRANSFER_MCC && existing.mcc == TRANSFER_MCC

            return amountMatch && currencyMatch && signMatch && mccMatch
        }

        private fun logIncoming(mappings: Mappings, webhookResponseData: MonoWebHookResponseData) {
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
