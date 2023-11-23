package io.github.smaugfm.monobudget.common.retry

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import org.koin.core.annotation.Single
import java.util.UUID
import kotlin.time.Duration

@Single
class InMemoryStatementRetryRepositoryImpl : StatementRetryRepository {
    private val map = mutableMapOf<RetryRequestId, StatementRetryRequest>()

    override suspend fun addRetryRequest(
        ctx: StatementProcessingContext,
        retryWaitDuration: Duration
    ): StatementRetryRequest =
        StatementRetryRequest(
            UUID.randomUUID().toString(), ctx, retryWaitDuration
        ).also {
            map[it.id] = it
        }

    override suspend fun removeRetryRequest(id: RetryRequestId) {
        map.remove(id)
    }

    override suspend fun getAllRequests() =
        map.values.toList()
}
