package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.Settings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency

@OptIn(ExperimentalSerializationApi::class)
class YnabCurrencyVerifier : ApplicationStartupVerifier, KoinComponent {
    private val budgetBackend: BudgetBackend.YNAB by inject()
    private val monoSettings: Settings.MultipleMonoSettings by inject()
    private val ynabApi: YnabApi by inject()

    override suspend fun verify() {
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
