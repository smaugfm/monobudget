package io.github.smaugfm.monobudget.common.lifecycle

import com.elbekd.bot.types.CallbackQuery
import io.github.smaugfm.monobudget.common.exception.BudgetBackendError
import io.github.smaugfm.monobudget.common.model.callback.CallbackType

sealed interface StatementProcessingEventListener {
    interface New : StatementProcessingEventListener {
        suspend fun handleNewStatement(ctx: StatementProcessingContext): Boolean
    }

    interface End : StatementProcessingEventListener {
        suspend fun handleStatementEnd(ctx: StatementProcessingContext)
    }

    interface Error : StatementProcessingEventListener {
        suspend fun handleStatementError(
            ctx: StatementProcessingContext,
            e: Throwable,
        ): Boolean
    }

    interface Retry : StatementProcessingEventListener {
        suspend fun handleRetry(
            ctx: StatementProcessingContext,
            e: BudgetBackendError,
        )
    }

    interface CallbackError : StatementProcessingEventListener {
        suspend fun handleCallbackError(
            query: CallbackQuery,
            callbackType: CallbackType?,
            e: Throwable,
        )
    }
}
