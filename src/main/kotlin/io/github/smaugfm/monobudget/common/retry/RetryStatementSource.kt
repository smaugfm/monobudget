package io.github.smaugfm.monobudget.common.retry

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
                ctx,
                retrySettings.interval,
            )
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
