package com.github.smaugfm.ynab.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.util.ExpiringMap
import com.github.smaugfm.ynab.UniqueWebhookResponses
import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.ynab.YnabCleared
import com.github.smaugfm.ynab.YnabSaveTransaction
import com.github.smaugfm.ynab.YnabTransactionDetail
import com.github.smaugfm.ynab.YnabTransferPayeeIdsCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import mu.KotlinLogging
import kotlin.math.abs
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class CreateTransactionHandler(
    private val ynab: YnabApi,
    private val mappings: Mappings,
) : Handler() {
    private val webhookResponseToYnabTransactionConverter = MonoWebhookResponseToYnabTransactionConverter(mappings) {
        ynab.getPayees()
    }
    private val transferPayeeIdsCache = YnabTransferPayeeIdsCache(ynab)
    private val recentTransactions =
        ExpiringMap<MonoStatementItem, Deferred<YnabTransactionDetail>>(Duration.minutes(1))
    private val incomingMutex = Mutex(false)

    private fun MonoWebHookResponseData.convertToYnab(): YnabSaveTransaction =
        webhookResponseToYnabTransactionConverter(this)

    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@CreateTransactionHandler::handle)
    }

    private suspend fun handle(
        dispatcher: IEventDispatcher,
        e: Event.Mono.WebHookQueried,
    ) {
        val webhookResponseData = e.data

        if (!UniqueWebhookResponses.isUnique(webhookResponseData)) {
            logger.info("Duplicate ${MonoWebHookResponseData::class.simpleName} $webhookResponseData")
            return
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

            dispatcher(
                Event.Telegram.SendStatementMessage(
                    webhookResponseData,
                    newTransaction
                )
            )
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
        ynab.createTransaction(webhookResponse.convertToYnab())

    companion object {
        private const val TRANSFER_MCC = 4829
    }
}
