package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.model.financial.Account
import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import java.util.Currency

abstract class BankAccountService {
    abstract suspend fun getAccounts(): List<Account>
    abstract fun getTelegramChatIdByAccountId(accountId: BankAccountId): Long?
    abstract fun getBudgetAccountId(accountId: BankAccountId): String?

    suspend fun getAccountAlias(accountId: BankAccountId): String? = getAccounts().find { it.id == accountId }?.alias
    suspend fun getAccountCurrency(accountId: BankAccountId): Currency? =
        getAccounts().find { it.id == accountId }?.currency
}
