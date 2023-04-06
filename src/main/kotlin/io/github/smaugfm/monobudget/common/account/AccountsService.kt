package io.github.smaugfm.monobudget.common.account

import io.github.smaugfm.monobudget.common.model.financial.Account
import java.util.Currency

abstract class AccountsService {
    abstract suspend fun getAccounts(): List<Account>
    abstract fun getTelegramChatIdAccByMono(monoAccountId: String): Long?
    abstract fun getBudgetAccountId(monoAccountId: String): String?

    suspend fun getAccountAlias(monoAccountId: String): String? = getAccounts().find { it.id == monoAccountId }?.alias

    suspend fun getAccountIds(): List<String> = getAccounts().map { it.id }

    suspend fun getAccountCurrency(monoAccountId: String): Currency? =
        getAccounts().find { it.id == monoAccountId }?.currency
}
