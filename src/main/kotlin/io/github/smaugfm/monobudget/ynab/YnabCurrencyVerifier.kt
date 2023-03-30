package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency

@Single
class YnabCurrencyVerifier : ApplicationStartupVerifier, KoinComponent {
    private val budgetBackend: BudgetBackend.YNAB by inject()
    private val monoSettings: Settings.MultipleMonoSettings by inject()
    private val service: MonoAccountsService by inject()
    private val ynabApi: YnabApi by inject()

    override suspend fun verify() {
        val budgetCurrency = ynabApi
            .getBudget(budgetBackend.ynabBudgetId)
            .currencyFormat
            .isoCode
            .let(Currency::getInstance)
        val realMonoAccounts = service.getAccounts()

        monoSettings
            .settings
            .map { it.accountId }
            .forEach { accountId ->
                val account = realMonoAccounts
                    .find { it.id == accountId }
                checkNotNull(account)
                check(account.currencyCode == budgetCurrency)
            }
    }
}
