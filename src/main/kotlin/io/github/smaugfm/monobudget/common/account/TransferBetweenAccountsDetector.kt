package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.misc.ExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.minutes

private val log = KotlinLogging.logger {}

open class TransferBetweenAccountsDetector<TTransaction> : KoinComponent {
    private val bankAccounts: BankAccountService by inject()

    private val recentTransactions =
        ExpiringMap<StatementItem, Deferred<TTransaction>>(1.minutes)

    sealed class MaybeTransfer<TTransaction> {
        abstract val statement: StatementItem
        data class Transfer<TTransaction>(
            override val statement: StatementItem,
            private val processed: TTransaction
        ) : MaybeTransfer<TTransaction>() {
            @Suppress("UNCHECKED_CAST")
            fun <T : Any> processed(): T {
                return processed as T
            }
        }

        class NotTransfer<TTransaction>(
            override val statement: StatementItem,
            private val processedDeferred: CompletableDeferred<TTransaction>
        ) : MaybeTransfer<TTransaction>() {
            @Volatile
            private var ran = false

            suspend fun consume(block: suspend (StatementItem) -> TTransaction): TTransaction {
                check(!ran) { "Can consume NotTransfer only once" }

                return block(statement).also {
                    processedDeferred.complete(it)
                    ran = true
                }
            }
        }
    }

    suspend fun checkTransfer(statement: StatementItem): MaybeTransfer<TTransaction> {
        val existingTransfer = recentTransactions.entries.firstOrNull { (recentStatementItem) ->
            checkIsTransferTransactions(
                statement,
                recentStatementItem
            )
        }?.value?.await()

        return if (existingTransfer != null) {
            log.debug {
                "Found matching transfer transaction.\n" +
                    "Current: $statement\n" +
                    "Recent transfer: $existingTransfer"
            }
            MaybeTransfer.Transfer(statement, existingTransfer)
        } else {
            val deferred = CompletableDeferred<TTransaction>()
            recentTransactions.add(statement, deferred)

            MaybeTransfer.NotTransfer(statement, deferred)
        }
    }

    private suspend fun checkIsTransferTransactions(new: StatementItem, existing: StatementItem): Boolean {
        val amountMatch = amountMatch(new, existing)
        val currencyMatch = currencyMatch(new, existing)
        val mccMatch = mccMatch(new, existing)

        return (amountMatch && currencyMatch && mccMatch).also {
            if (!it) {
                log.debug {
                    "Not transfer " +
                        "new=$new\nand existing=$existing\n" +
                        "amountMatch=$amountMatch, currencyMatch=$currencyMatch, mccMatch=$mccMatch"
                }
            }
        }
    }

    private suspend fun currencyMatch(new: StatementItem, existing: StatementItem): Boolean {
        val newTransactionAccountCurrency = bankAccounts.getAccountCurrency(new.accountId)
        val existingTransactionAccountCurrency = bankAccounts.getAccountCurrency(existing.accountId)
        return new.currency == existing.currency ||
            newTransactionAccountCurrency == existing.currency ||
            existingTransactionAccountCurrency == new.currency
    }

    private fun amountMatch(new: StatementItem, existing: StatementItem): Boolean {
        val a1 = new.amount
        val a2 = existing.amount
        val oa1 = new.operationAmount
        val oa2 = existing.operationAmount

        if (a1.equalsInverted(a2)) {
            return true
        }

        return a1.equalsInverted(oa2) || oa1.equalsInverted(a2)
    }

    private fun mccMatch(new: StatementItem, existing: StatementItem) = new.mcc == existing.mcc
}
