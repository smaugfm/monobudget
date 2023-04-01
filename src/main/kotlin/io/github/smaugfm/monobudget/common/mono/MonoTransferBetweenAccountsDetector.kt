package io.github.smaugfm.monobudget.common.mono

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.misc.ExpiringMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger {}

open class MonoTransferBetweenAccountsDetector<TTransaction> : KoinComponent {
    private val monoAccountsService: MonoAccountsService by inject()

    private val recentTransactions =
        ExpiringMap<MonoWebhookResponseData, Deferred<TTransaction>>(1.minutes)

    sealed class MaybeTransfer<TTransaction> {
        data class Transfer<TTransaction>(
            val webhookResponse: MonoWebhookResponseData,
            private val processed: TTransaction
        ) : MaybeTransfer<TTransaction>() {
            @Suppress("UNCHECKED_CAST")
            fun <T : Any> processed(): T {
                return processed as T
            }
        }

        class NotTransfer<TTransaction>(
            private val webhookResponse: MonoWebhookResponseData,
            private val processedDeferred: CompletableDeferred<TTransaction>
        ) : MaybeTransfer<TTransaction>() {
            @Volatile
            private var ran = false

            suspend fun consume(block: suspend (MonoWebhookResponseData) -> TTransaction): TTransaction {
                check(!ran) { "Can consume NotTransfer only once" }

                return block(webhookResponse).also {
                    processedDeferred.complete(it)
                    ran = true
                }
            }
        }
    }

    suspend fun checkTransfer(webhookResponseData: MonoWebhookResponseData): MaybeTransfer<TTransaction> {
        val existingTransfer = recentTransactions.entries.firstOrNull { (recentStatementItem) ->
            checkIsTransferTransactions(
                webhookResponseData,
                recentStatementItem
            ).also {
                if (it) {
                    log.debug { "Found transfer. Waiting for deferred to finish." }
                }
            }
        }?.value?.await()

        return if (existingTransfer != null) {
            log.debug {
                "Found matching transfer transaction.\n" +
                    "Current: $webhookResponseData\n" +
                    "Recent transfer: $existingTransfer"
            }
            MaybeTransfer.Transfer(webhookResponseData, existingTransfer)
        } else {
            val deferred = CompletableDeferred<TTransaction>()
            recentTransactions.add(webhookResponseData, deferred)

            MaybeTransfer.NotTransfer(webhookResponseData, deferred)
        }
    }

    private suspend fun checkIsTransferTransactions(
        new: MonoWebhookResponseData,
        existing: MonoWebhookResponseData
    ): Boolean {
        val amountMatch = amountMatch(new.statementItem, existing.statementItem)
        val currencyMatch = currencyMatch(new, existing)
        val mccMatch = mccMatch(new, existing)

        return amountMatch && currencyMatch && mccMatch
    }

    private suspend fun currencyMatch(new: MonoWebhookResponseData, existing: MonoWebhookResponseData): Boolean {
        val newTransactionAccountCurrency = monoAccountsService.getAccountCurrency(new.account)
        val existingTransactionAccountCurrency = monoAccountsService.getAccountCurrency(existing.account)
        return new.statementItem.currencyCode == existing.statementItem.currencyCode ||
            newTransactionAccountCurrency == existing.statementItem.currencyCode ||
            existingTransactionAccountCurrency == new.statementItem.currencyCode
    }

    private fun amountMatch(new: MonoStatementItem, existing: MonoStatementItem): Boolean {
        val a1 = new.amount
        val a2 = existing.amount
        val oa1 = new.operationAmount
        val oa2 = existing.operationAmount

        if (a1.equalsInverted(a2)) {
            return true
        }

        return a1.equalsInverted(oa2) || oa1.equalsInverted(a2)
    }

    private fun mccMatch(new: MonoWebhookResponseData, existing: MonoWebhookResponseData) =
        new.statementItem.mcc == TRANSFER_MCC && existing.statementItem.mcc == TRANSFER_MCC

    companion object {
        private const val TRANSFER_MCC = 4829

        fun Long.equalsInverted(other: Long): Boolean = this == -other
    }
}
