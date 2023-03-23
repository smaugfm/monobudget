package io.github.smaugfm.monobudget.components.verification

import io.github.smaugfm.monobudget.model.Settings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalSerializationApi::class)
class MonoSettingsVerifier(
    private val monoSettings: Settings.MultipleMonoSettings,
) : ApplicationStartupVerifier {
    override suspend fun verify() {
        monoSettings
            .apis
            .zip(monoSettings.settings.map { it.accountId })
            .forEach { (api, accountId) ->
                check(api.api
                    .getClientInformation()
                    .awaitSingle()
                    .accounts
                    .any {
                        it.id == accountId
                    }) {
                    "Failed to find accountId=$accountId in Monobank client information"
                }
            }
    }
}
