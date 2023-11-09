package io.github.smaugfm.monobudget.common.exception

import io.github.smaugfm.monobudget.common.model.financial.BankAccountId

class BudgetBackendError(
    cause: Throwable,
    val bankAccountId: BankAccountId,
    val userMessage: String
) : Exception(cause)
