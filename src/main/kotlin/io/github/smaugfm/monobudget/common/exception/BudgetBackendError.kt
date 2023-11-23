package io.github.smaugfm.monobudget.common.exception

class BudgetBackendError(
    cause: Throwable,
    val userMessage: String
) : Exception(cause)
