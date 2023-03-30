package io.github.smaugfm.monobudget.common.mono

import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class MonoSettingsVerifier : ApplicationStartupVerifier, KoinComponent {
    private val monoSettings: Settings.MultipleMonoSettings by inject()
    private val service: MonoAccountsService by inject()

    override suspend fun verify() {
        val realAccountsIds = service.getAccounts().map { it.id }
        monoSettings.settings.map { it.accountId }
            .forEach { accountId ->
                check(accountId in realAccountsIds) {
                    "Failed to find accountId=$accountId in Monobank client information"
                }
            }
    }
}
