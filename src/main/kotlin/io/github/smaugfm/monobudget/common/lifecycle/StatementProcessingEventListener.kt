package io.github.smaugfm.monobudget.common.lifecycle

sealed interface StatementProcessingEventListener {
    interface New : StatementProcessingEventListener {
        suspend fun handleNewStatement(ctx: StatementProcessingContext): Boolean
    }

    interface End : StatementProcessingEventListener {
        suspend fun handleProcessingEnd(ctx: StatementProcessingContext)
    }

    interface Error : StatementProcessingEventListener {
        suspend fun handleProcessingError(ctx: StatementProcessingContext, e: Throwable)
    }
}
