package io.github.smaugfm.monobudget.common.verify

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.ynab.YnabApi
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent

private val log = KotlinLogging.logger { }

@Single
class BudgetSettingsVerifier(
    private val budgetBackend: BudgetBackend,
    private val accountSettings: MultipleAccountSettings
) : ApplicationStartupVerifier, KoinComponent {

    override suspend fun verify() {
        when (budgetBackend) {
            is BudgetBackend.Lunchmoney -> {
                val api = getKoin().get<LunchmoneyApi>()
                accountSettings.settings.forEach { settings ->
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
                accountSettings.settings.forEach { settings ->
                    try {
                        api.getAccount(settings.budgetAccountId)
                    } catch (e: Throwable) {
                        log.error(e) {}
                        error("Failed to find YNAB account with id=${settings.budgetAccountId}")
                    }
                }
            }
        }
    }
}
