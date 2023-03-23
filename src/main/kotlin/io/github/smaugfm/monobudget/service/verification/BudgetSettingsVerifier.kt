package io.github.smaugfm.monobudget.service.verification

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.request.asset.LunchmoneyGetAllAssetsRequest
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.BudgetBackend
import io.github.smaugfm.monobudget.models.Settings
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.component.KoinComponent

class BudgetSettingsVerifier(
    private val budgetBackend: BudgetBackend,
    private val monoSettings: Settings.MultipleMonoSettings
) : ApplicationStartupVerifier, KoinComponent {
    override suspend fun verify() {
        when (budgetBackend) {
            is BudgetBackend.Lunchmoney -> {
                val api = getKoin().get<LunchmoneyApi>()
                monoSettings.settings.forEach { settings ->
                    check(api
                        .execute(LunchmoneyGetAllAssetsRequest())
                        .awaitSingle()
                        .assets
                        .any {
                            it.id.toString() == settings.budgetAccountId
                        }) {
                        "Failed to find Lunchmoney account with id=${settings.budgetAccountId}"
                    }
                }
            }

            is BudgetBackend.YNAB -> {
                val api = getKoin().get<YnabApi>()
                monoSettings.settings.forEach { settings ->
                    try {
                        api
                            .getAccount(settings.budgetAccountId)
                    } catch (e: Throwable) {
                        error("Failed to find YNAB account with id=${settings.budgetAccountId}")
                    }
                }
            }
        }
    }
}
