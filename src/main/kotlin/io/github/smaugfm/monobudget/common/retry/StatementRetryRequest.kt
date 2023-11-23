package io.github.smaugfm.monobudget.common.retry

import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class StatementRetryRequest(
    val id: RetryRequestId,
    val ctx: StatementProcessingContext,
    val retryAt: Instant,
) {
    constructor(id: RetryRequestId, ctx: StatementProcessingContext, retryIn: Duration) : this(
        id,
        ctx,
        Clock.System.now() + retryIn,
    )

    val retryIn get() = (retryAt - Clock.System.now()).coerceAtLeast(Duration.ZERO)
}
