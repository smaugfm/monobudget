package io.github.smaugfm.monobudget.common.transaction

import io.github.smaugfm.monobudget.common.account.MaybeTransfer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class TransactionFactory<TTransaction, TNewTransaction> : KoinComponent {
    protected val newTransactionFactory: NewTransactionFactory<TNewTransaction> by inject()

    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
