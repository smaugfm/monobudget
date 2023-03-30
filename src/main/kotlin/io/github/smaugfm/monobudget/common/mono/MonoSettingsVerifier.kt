package io.github.smaugfm.monobudget.common.mono

import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class MonoSettingsVerifier : ApplicationStartupVerifier, KoinComponent {
    private val monoSettings: io.github.smaugfm.monobudget.common.model.Settings.MultipleMonoSettings by inject()

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
