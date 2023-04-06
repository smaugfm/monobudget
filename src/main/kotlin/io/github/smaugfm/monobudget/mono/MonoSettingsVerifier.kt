package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import org.koin.core.annotation.Single

@Single
class MonoSettingsVerifier(
    private val monoSettings: Settings.MultipleMonoSettings,
    private val accounts: MonoAccountsService
) : ApplicationStartupVerifier {

    override suspend fun verify() {
        val realAccountsIds = accounts.getAccountIds()
        monoSettings.settings.map { it.accountId }
            .forEach { accountId ->
                check(accountId in realAccountsIds) {
                    "Failed to find accountId=$accountId in Monobank client information"
                }
            }
    }
}
