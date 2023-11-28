package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.CompletableDeferred

sealed class MaybeTransfer<TTransaction> {
    abstract val statement: StatementItem

    data class Transfer<TTransaction>(
        override val statement: StatementItem,
        private val processed: TTransaction,
    ) : MaybeTransfer<TTransaction>() {
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> processed(): T {
            return processed as T
        }
    }

    class NotTransfer<TTransaction>(
        override val statement: StatementItem,
        private val processedDeferred: CompletableDeferred<TTransaction>,
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
