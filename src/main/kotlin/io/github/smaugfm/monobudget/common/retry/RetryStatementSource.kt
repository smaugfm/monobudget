package io.github.smaugfm.monobudget.common.retry

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingEventListener
import io.github.smaugfm.monobudget.common.model.settings.RetrySettings
import io.github.smaugfm.monobudget.common.statement.StatementSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

@Single
class RetryStatementSource(
    private val scope: CoroutineScope,
    private val retrySettings: RetrySettings,
) : StatementProcessingEventListener.Retry, StatementSource, KoinComponent {
    private val repository: StatementRetryRepository by inject()
    private val flow = MutableSharedFlow<StatementProcessingContext>()

    override suspend fun prepare() {
        val requests = repository.getAllRequests()
        requests.forEach(::scheduleRetry)
    }

    override suspend fun handleRetry(
        ctx: StatementProcessingContext,
        e: BudgetBackendError,
    ) {
        val request =
            repository.addRetryRequest(
                ctx.incrementAttempt(),
                retrySettings.interval,
            )
        log.warn(e) { "Error processing transaction. Will retry in ${retrySettings.interval}..." }
        scheduleRetry(request)
    }

    private fun scheduleRetry(request: StatementRetryRequest) {
        with(request) {
            scope.launch {
                delay(retryIn)
                flow.emit(ctx)
                repository.removeRetryRequest(id)
            }
        }
    }

    override suspend fun statements() = flow
}
