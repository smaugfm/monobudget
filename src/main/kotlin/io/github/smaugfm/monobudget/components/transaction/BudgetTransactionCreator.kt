package io.github.smaugfm.monobudget.components.transaction

import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.components.transaction.factory.NewTransactionFactory

sealed class BudgetTransactionCreator<TTransaction, TNewTransaction>(
    protected val newTransactionFactory: NewTransactionFactory<TNewTransaction>
) {
    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
