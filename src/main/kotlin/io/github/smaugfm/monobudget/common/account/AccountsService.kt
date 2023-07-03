package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.model.financial.Account
import java.util.Currency

abstract class AccountsService {
    abstract suspend fun getAccounts(): List<Account>
    abstract fun getTelegramChatIdByAccountId(accountId: String): Long?

    abstract fun getBudgetAccountId(accountId: String): String?
    suspend fun getAccountAlias(accountId: String): String? = getAccounts().find { it.id == accountId }?.alias
    suspend fun getAccountCurrency(accountId: String): Currency? =
        getAccounts().find { it.id == accountId }?.currency
}
