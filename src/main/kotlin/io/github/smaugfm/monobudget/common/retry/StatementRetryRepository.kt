package io.github.smaugfm.monobudget.common.retry

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import kotlin.time.Duration

interface StatementRetryRepository {
    suspend fun addRetryRequest(
        ctx: StatementProcessingContext,
        retryWaitDuration: Duration,
    ): StatementRetryRequest

    suspend fun removeRetryRequest(id: RetryRequestId)

    suspend fun getAllRequests(): List<StatementRetryRequest>
}

typealias RetryRequestId = String
