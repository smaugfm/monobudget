package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.Settings
import io.ktor.util.logging.error
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger { }

class BudgetSettingsVerifier : ApplicationStartupVerifier, KoinComponent {
    private val budgetBackend: BudgetBackend by inject()
    private val monoSettings: Settings.MultipleMonoSettings by inject()

    override suspend fun verify() {
        when (budgetBackend) {
            is BudgetBackend.Lunchmoney -> {
                val api = getKoin().get<LunchmoneyApi>()
                monoSettings.settings.forEach { settings ->
                    check(
                        api.getAllAssets()
                            .awaitSingle()
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
