package io.github.smaugfm.monobudget.common.account

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

private val log = KotlinLogging.logger {}

abstract class TransferBetweenAccountsDetector<TTransaction>(
    private val bankAccounts: BankAccountService,
    private val statementItem: StatementItem,
    private val cache: ConcurrentExpiringMap<StatementItem, Deferred<TTransaction>>
) {
    suspend fun checkTransfer(): MaybeTransferStatement<TTransaction> {
        val existingTransfer = cache.entries.firstOrNull { (recentStatementItem) ->
            checkIsTransferTransactions(recentStatementItem)
        }?.value?.await()

        return if (existingTransfer != null) {
            log.debug {
                "Found matching transfer transaction.\n" +
                    "Current: $statementItem\n" +
                    "Recent transfer: $existingTransfer"
            }
            MaybeTransferStatement.Transfer(statementItem, existingTransfer)
        } else {
            val deferred = CompletableDeferred<TTransaction>()
            cache.add(statementItem, deferred)

            MaybeTransferStatement.NotTransfer(statementItem, deferred)
        }
    }

    private suspend fun checkIsTransferTransactions(existing: StatementItem): Boolean {
        val new = statementItem

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
