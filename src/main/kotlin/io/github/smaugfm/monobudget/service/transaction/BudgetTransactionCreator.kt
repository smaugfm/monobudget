package io.github.smaugfm.monobudget.service.transaction

import io.github.smaugfm.monobudget.service.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.service.transaction.factory.NewTransactionFactory

sealed class BudgetTransactionCreator<TTransaction, TNewTransaction>(
    protected val newTransactionFactory: NewTransactionFactory<TNewTransaction>
) {
    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
