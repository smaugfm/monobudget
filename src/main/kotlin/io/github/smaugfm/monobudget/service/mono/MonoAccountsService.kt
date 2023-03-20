package io.github.smaugfm.monobudget.service.mono

import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import java.util.Currency

private val logger = KotlinLogging.logger { }

@OptIn(ExperimentalSerializationApi::class)
class MonoAccountsService(
    fetcherFactory: PeriodicFetcherFactory,
    private val settings: Settings.MultipleMonoSettings,
) {
    private val monoAccountsFetcher = fetcherFactory.create(this::class.simpleName!!) {
        settings.apis
            .mapIndexed { index, api ->
                val accountId = settings.settings[index].accountId
                api.api.getClientInformation().awaitSingle().accounts.firstOrNull { it.id == accountId }!!
            }
    }

    private fun <T : Any?> T.logMissing(name: String, monoAccountId: String): T {
        if (this == null)
            logger.error { "Could not find alias for $name for Monobank accountId=$monoAccountId" }
        return this
    }

    fun getMonoAccAlias(monoAccountId: String): String? =
        settings.byId[monoAccountId]
            ?.alias
            .logMissing("Monobank account", monoAccountId)

    suspend fun getAccountCurrency(monoAccountId: String): Currency? =
        monoAccountsFetcher.data.await().firstOrNull { it.id == monoAccountId }?.currencyCode

    fun getTelegramChatIdAccByMono(monoAccountId: String) =
        settings.byId[monoAccountId]
            ?.telegramChatId
            .logMissing("Telegram chat ID", monoAccountId)

    fun getBudgetAccountId(monoAccountId: String): String? =
        settings.byId[monoAccountId]
            ?.budgetAccountId
            .logMissing("Budget account ID", monoAccountId)
}
