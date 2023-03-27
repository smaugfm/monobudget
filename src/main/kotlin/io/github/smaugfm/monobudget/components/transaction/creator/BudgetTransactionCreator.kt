package io.github.smaugfm.monobudget.components.transaction.creator

import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer
import io.github.smaugfm.monobudget.components.transaction.factory.NewTransactionFactory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BudgetTransactionCreator<TTransaction, TNewTransaction> : KoinComponent {
    protected val newTransactionFactory: NewTransactionFactory<TNewTransaction> by inject()

    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
