package io.github.smaugfm.monobudget.common.retry

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
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

    val retryIn
        @JsonIgnore
        get() = (retryAt - Clock.System.now()).coerceAtLeast(Duration.ZERO)
}
