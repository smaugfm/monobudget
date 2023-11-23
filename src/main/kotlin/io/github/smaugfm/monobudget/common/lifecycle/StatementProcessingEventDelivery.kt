package io.github.smaugfm.monobudget.common.lifecycle

import com.elbekd.bot.types.CallbackQuery
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.model.callback.CallbackType
import io.github.smaugfm.monobudget.common.util.injectAll
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent

private val log = KotlinLogging.logger {}

@Single
class StatementProcessingEventDelivery : KoinComponent {
    private val newStatementListeners
        by injectAll<StatementProcessingEventListener.New>()
    private val statementEndListeners
        by injectAll<StatementProcessingEventListener.End>()
    private val statementErrorListeners
        by injectAll<StatementProcessingEventListener.Error>()
    private val callbackErrorListeners
        by injectAll<StatementProcessingEventListener.CallbackError>()
    private val retryScheduledEventListeners
        by injectAll<StatementProcessingEventListener.Retry>()

    suspend fun onNewStatement(ctx: StatementProcessingContext): Boolean =
        newStatementListeners.all { l ->
            l.handleNewStatement(ctx)
        }

    suspend fun onStatementEnd(ctx: StatementProcessingContext) {
        statementEndListeners.forEach { l ->
            l.handleStatementEnd(ctx)
        }
    }

    suspend fun onStatementError(
        ctx: StatementProcessingContext,
        e: Throwable,
    ) {
        log.error(e) {}
        statementErrorListeners.all { l ->
            l.handleStatementError(ctx, e)
        }
    }

    suspend fun onCallbackError(
        query: CallbackQuery,
        callbackType: CallbackType?,
        e: Throwable,
    ) {
        log.error(e) {}
        callbackErrorListeners.forEach { l ->
            l.handleCallbackError(query, callbackType, e)
        }
    }

    suspend fun onStatementRetry(
        ctx: StatementProcessingContext,
        e: BudgetBackendError,
    ) {
        retryScheduledEventListeners.forEach { l ->
            l.handleRetry(ctx, e)
        }
    }
}
