package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.request.asset.LunchmoneyGetAllAssetsRequest
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.Settings
import io.ktor.util.logging.error
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.koin.core.component.KoinComponent

private val log = KotlinLogging.logger { }

class BudgetSettingsVerifier(
    private val budgetBackend: BudgetBackend,
    private val monoSettings: Settings.MultipleMonoSettings
) : ApplicationStartupVerifier, KoinComponent {
    override suspend fun verify() {
        when (budgetBackend) {
            is BudgetBackend.Lunchmoney -> {
                val api = getKoin().get<LunchmoneyApi>()
                monoSettings.settings.forEach { settings ->
                    check(
                        api
                            .execute(LunchmoneyGetAllAssetsRequest())
                            .awaitSingle()
                            .assets
                            .any {
                                it.id.toString() == settings.budgetAccountId
                            }
                    ) {
                        "Failed to find Lunchmoney account with id=${settings.budgetAccountId}"
                    }
                }
            }

            is BudgetBackend.YNAB -> {
                val api = getKoin().get<YnabApi>()
                monoSettings.settings.forEach { settings ->
                    try {
                        api.getAccount(settings.budgetAccountId)
                    } catch (e: Throwable) {
                        log.error(e)
                        error("Failed to find YNAB account with id=${settings.budgetAccountId}")
                    }
                }
            }
        }
    }
}
