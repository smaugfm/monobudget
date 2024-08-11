package io.github.smaugfm.monobudget.import

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.TransferCache
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import kotlin.time.Duration

@OptIn(DelicateCoroutinesApi::class)
class ImporterTransferCache(private val cacheTimeSimulatedDuration: Duration) :
    TransferCache<LunchmoneyTransaction> {
    private val singleThreadedContext = newSingleThreadContext("import-transfer-cache")
    private val cache =
        mutableMapOf<StatementItem, Deferred<LunchmoneyTransaction>>()
            .toSortedMap { o1, o2 -> o1.time.compareTo(o2.time) }

    @Suppress("DeferredResultUnused")
    override suspend fun getEntries(
        item: StatementItem,
    ): Set<Map.Entry<StatementItem, Deferred<LunchmoneyTransaction>>> =
        withContext(singleThreadedContext) {
            val threshold = item.time - cacheTimeSimulatedDuration
            for (key in cache.keys.filter { it.time < threshold }) {
                cache.remove(key)
            }
            cache.entries
        }

    override suspend fun put(
        item: StatementItem,
        transaction: Deferred<LunchmoneyTransaction>,
    ) {
        check(cache.keys.all { item.time >= it.time })
        cache[item] = transaction
    }
}
