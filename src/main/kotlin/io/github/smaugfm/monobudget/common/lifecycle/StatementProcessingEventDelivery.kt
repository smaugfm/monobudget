package io.github.smaugfm.monobudget.common.lifecycle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
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

    suspend fun onNewStatement(item: StatementItem): Boolean = newStatementListeners.all { l ->
        l.handleNewStatement(item)
    }

    suspend fun onStatementEnd(item: StatementItem) {
        statementEndListeners.forEach { l ->
            l.handleProcessingEnd(item)
        }
    }

    suspend fun onStatementError(item: StatementItem, throwable: Throwable) {
        log.error(throwable) {}
        statementErrorListeners.forEach { l ->
            l.handleProcessingError(item, throwable)
        }
    }
}
