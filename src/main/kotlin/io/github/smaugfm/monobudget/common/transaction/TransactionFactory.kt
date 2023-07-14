package io.github.smaugfm.monobudget.common.transaction

import io.github.resilience4j.retry.Retry
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector.MaybeTransfer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class TransactionFactory<TTransaction, TNewTransaction> : KoinComponent {
    protected val newTransactionFactory: NewTransactionFactory<TNewTransaction> by inject()
    protected val apiRetry: Retry by inject()

    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
