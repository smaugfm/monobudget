package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.Settings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import java.util.Currency

@OptIn(ExperimentalSerializationApi::class)
class YnabCurrencyVerifier(
    private val budgetBackend: BudgetBackend,
    private val monoSettings: Settings.MultipleMonoSettings,
    private val ynabApi: YnabApi
) : ApplicationStartupVerifier {
    override suspend fun verify() {
        if (budgetBackend !is BudgetBackend.YNAB) {
            return
        }

        val budgetCurrency = ynabApi
            .getBudget(budgetBackend.ynabBudgetId)
            .currencyFormat
            .isoCode
            .let(Currency::getInstance)

        monoSettings
            .settings
            .map { it.accountId }
            .zip(monoSettings.apis)
            .forEach { (accountId, api) ->
                val account = api.api
                    .getClientInformation()
                    .awaitSingle()
                    .accounts
                    .find { it.id == accountId }
                checkNotNull(account)
                check(account.currencyCode == budgetCurrency)
            }
    }
}
