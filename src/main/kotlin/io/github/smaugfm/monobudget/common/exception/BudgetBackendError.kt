package io.github.smaugfm.monobudget.common.exception

sealed class BudgetBackendError(cause: Throwable, val userMessage: String) : Exception(cause) {
    class BudgetBackendApiError(cause: Throwable, userMessage: String) :
        BudgetBackendError(cause, userMessage)
    class BudgetBackendClientError(cause: Throwable, userMessage: String) :
        BudgetBackendError(cause, userMessage)
}
