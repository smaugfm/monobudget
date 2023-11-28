package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.common.startup.ApplicationStartupVerifier
import org.koin.core.annotation.Single

@Single
class MonoSettingsVerifier(
    private val monoSettings: MultipleAccountSettings,
    private val accounts: MonoAccountsService,
) : ApplicationStartupVerifier {
    override suspend fun verify() {
        check(
            monoSettings.accountIds.toSet()
                .containsAll(accounts.getAccounts().map { it.id }),
        ) {
            "Not all Monobank accounts exist"
        }
    }
}
