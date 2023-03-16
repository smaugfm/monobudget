package com.github.smaugfm.service

import com.github.smaugfm.util.ExpiringMap
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class MonoTransferBetweenAccountsDetector<T> {

    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<T>>(1.minutes)

    sealed class MaybeTransfer<T> {
        data class Transfer<T>(
            val webhookResponse: MonoWebhookResponseData,
            val processed: T,
        ) : MaybeTransfer<T>()

        class NotTransfer<T>(
            private val webhookResponse: MonoWebhookResponseData,
            private val processedDeferred: CompletableDeferred<T>
        ) : MaybeTransfer<T>() {
            @Volatile
            private var ran = false

            suspend fun consume(block: suspend (MonoWebhookResponseData) -> T): T {
                if (ran)
                    throw IllegalStateException("Can consume NotTransfer only once")

                return block(webhookResponse).also {
                    processedDeferred.complete(it)
                    ran = true
                }
            }
        }
    }

    suspend fun checkTransfer(webhookResponseData: MonoWebhookResponseData): MaybeTransfer<T> {
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

        return if (existingTransfer != null) {
            logger.debug {
                "Found matching transfer transaction.\n" +
                    "Current: $webhookResponseData\n" +
                    "Recent transfer: $existingTransfer"
            }
            MaybeTransfer.Transfer(webhookResponseData, existingTransfer)
        } else {
            val deferred = CompletableDeferred<T>()
            recentTransactions.add(webhookResponseData.statementItem, deferred)

            MaybeTransfer.NotTransfer(webhookResponseData, deferred)
        }
    }

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

    companion object {
        private const val TRANSFER_MCC = 4829
    }
}
