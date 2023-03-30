package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency

@OptIn(ExperimentalSerializationApi::class)
class YnabCurrencyVerifier : ApplicationStartupVerifier, KoinComponent {
    private val budgetBackend: io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB by inject()
    private val monoSettings: io.github.smaugfm.monobudget.common.model.Settings.MultipleMonoSettings by inject()
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
