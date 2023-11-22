package io.github.smaugfm.monobudget.common.lifecycle

import io.github.oshai.kotlinlogging.KotlinLogging
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

    suspend fun onNewStatement(ctx: StatementProcessingContext): Boolean = newStatementListeners.all { l ->
        l.handleNewStatement(ctx)
    }

    suspend fun onStatementEnd(ctx: StatementProcessingContext) {
        statementEndListeners.forEach { l ->
            l.handleProcessingEnd(ctx)
        }
    }

    suspend fun onStatementError(ctx: StatementProcessingContext, throwable: Throwable) {
        log.error(throwable) {}
        statementErrorListeners.forEach { l ->
            l.handleProcessingError(ctx, throwable)
        }
    }
}
