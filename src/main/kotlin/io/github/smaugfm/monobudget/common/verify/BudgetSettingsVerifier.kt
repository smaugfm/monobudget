package io.github.smaugfm.monobudget.common.verify

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.Settings.MultipleMonoSettings
import io.github.smaugfm.monobudget.ynab.YnabApi
import io.ktor.util.logging.error
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent

private val log = KotlinLogging.logger { }

@Single
class BudgetSettingsVerifier(
    private val budgetBackend: BudgetBackend,
    private val monoSettings: MultipleMonoSettings
) : ApplicationStartupVerifier, KoinComponent {

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
