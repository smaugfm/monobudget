package io.github.smaugfm.monobudget.common.lifecycle

import io.github.smaugfm.monobudget.common.model.financial.StatementItem

sealed interface StatementProcessingEventListener {
    interface New : StatementProcessingEventListener {
        suspend fun handleNewStatement(statementItem: StatementItem): Boolean
    }

    interface End : StatementProcessingEventListener {
        suspend fun handleProcessingEnd(statementItem: StatementItem)
    }

    interface Error : StatementProcessingEventListener {
        suspend fun handleProcessingError(item: StatementItem, e: Throwable)
    }
}
