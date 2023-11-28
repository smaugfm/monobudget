package io.github.smaugfm.monobudget.common.exception

class BudgetBackendException(
    cause: Throwable,
    val userMessage: String,
) : Exception(cause)
