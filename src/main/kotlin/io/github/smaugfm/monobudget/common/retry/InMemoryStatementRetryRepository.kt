package io.github.smaugfm.monobudget.common.retry

import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import java.util.UUID
import kotlin.time.Duration

class InMemoryStatementRetryRepository : StatementRetryRepository {
    private val map = mutableMapOf<RetryRequestId, StatementRetryRequest>()

    override suspend fun addRetryRequest(
        ctx: StatementProcessingContext,
        retryWaitDuration: Duration,
    ): StatementRetryRequest =
        StatementRetryRequest(
            UUID.randomUUID().toString(),
            ctx,
            retryWaitDuration,
        ).also {
            map[it.id] = it
        }

    override suspend fun removeRetryRequest(id: RetryRequestId) {
        map.remove(id)
    }

    override suspend fun getAllRequests() = map.values.toList()
}
