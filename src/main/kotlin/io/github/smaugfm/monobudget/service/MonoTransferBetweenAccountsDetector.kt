package io.github.smaugfm.monobudget.service

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.util.ExpiringMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

class MonoTransferBetweenAccountsDetector {

    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<Any>>(1.minutes)

    sealed class MaybeTransfer {
        data class Transfer(
            val webhookResponse: MonoWebhookResponseData,
            private val processed: Any
        ) : MaybeTransfer() {
            @Suppress("UNCHECKED_CAST")
            fun <T : Any> processed(): T {
                return processed as T
            }
        }

        class NotTransfer(
            private val webhookResponse: MonoWebhookResponseData,
            private val processedDeferred: CompletableDeferred<Any>
        ) : MaybeTransfer() {
            @Volatile
            private var ran = false

            suspend fun <T : Any> consume(block: suspend (MonoWebhookResponseData) -> T): T {
                check(!ran) { "Can consume NotTransfer only once" }

                return block(webhookResponse).also {
                    processedDeferred.complete(it)
                    ran = true
                }
            }
        }
    }

    suspend fun checkTransfer(webhookResponseData: MonoWebhookResponseData): MaybeTransfer {
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
            val deferred = CompletableDeferred<Any>()
            recentTransactions.add(webhookResponseData.statementItem, deferred)

            MaybeTransfer.NotTransfer(webhookResponseData, deferred)
        }
    }

    private fun checkIsTransferTransactions(new: MonoStatementItem, existing: MonoStatementItem): Boolean {
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
