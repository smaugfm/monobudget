package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.monobudget.model.Settings
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MonoSettingsVerifier : ApplicationStartupVerifier, KoinComponent {
    private val monoSettings: Settings.MultipleMonoSettings by inject()

    override suspend fun verify() {
        monoSettings
            .apis
            .zip(monoSettings.settings.map { it.accountId })
            .forEach { (api, accountId) ->
                check(
                    api.api
                        .getClientInformation()
                        .awaitSingle()
                        .accounts
                        .any {
                            it.id == accountId
                        }
                ) {
                    "Failed to find accountId=$accountId in Monobank client information"
                }
            }
    }
}
