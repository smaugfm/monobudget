package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.util.misc.ConcurrentExpiringMap
import kotlinx.coroutines.Deferred
import kotlin.time.Duration

interface TransferCache<TTransaction> {
    suspend fun getEntries(item: StatementItem): Set<Map.Entry<StatementItem, Deferred<TTransaction>>>

    suspend fun put(
        item: StatementItem,
        transaction: Deferred<TTransaction>,
    )

    open class Expiring<TTransaction>(expirationDuration: Duration) :
        TransferCache<TTransaction> {
        private val cache = ConcurrentExpiringMap<StatementItem, Deferred<TTransaction>>(expirationDuration)

        override suspend fun getEntries(
            item: StatementItem,
        ): Set<Map.Entry<StatementItem, Deferred<TTransaction>>> {
            return cache.entries
        }

        override suspend fun put(
            item: StatementItem,
            transaction: Deferred<TTransaction>,
        ) {
            cache.add(item, transaction)
        }
    }
}
