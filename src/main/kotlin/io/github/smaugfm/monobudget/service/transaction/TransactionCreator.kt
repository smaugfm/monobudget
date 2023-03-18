package io.github.smaugfm.monobudget.service.transaction

import io.github.smaugfm.monobudget.service.mono.MonoTransferBetweenAccountsDetector.MaybeTransfer

sealed class TransactionCreator<TTransaction> {

    abstract suspend fun create(maybeTransfer: MaybeTransfer<TTransaction>): TTransaction
}
