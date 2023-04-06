package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import org.koin.core.annotation.Single
import java.util.Currency

@Single
class YnabCurrencyVerifier(
    private val budgetBackend: BudgetBackend.YNAB,
    private val monoSettings: Settings.MultipleMonoSettings,
    private val accounts: AccountsService,
    private val ynabApi: YnabApi
) : ApplicationStartupVerifier {

    override suspend fun verify() {
        val budgetCurrency = ynabApi
            .getBudget(budgetBackend.ynabBudgetId)
            .currencyFormat
            .isoCode
            .let(Currency::getInstance)
        val monoAccounts = accounts.getAccounts()

        monoSettings
            .settings
            .map { it.accountId }
            .forEach { accountId ->
                val account = monoAccounts
                    .find { it.id == accountId }
                checkNotNull(account)
                check(account.currency == budgetCurrency)
            }
    }
}
