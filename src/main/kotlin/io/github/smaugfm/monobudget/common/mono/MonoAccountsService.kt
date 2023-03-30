package io.github.smaugfm.monobudget.common.mono

import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.Settings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.Currency

private val log = KotlinLogging.logger { }

@Single(createdAtStart = true)
@OptIn(ExperimentalSerializationApi::class)
class MonoAccountsService : KoinComponent {
    private val fetcherFactory: PeriodicFetcherFactory by inject()
    private val settings: Settings.MultipleMonoSettings by inject()

    private val monoAccountsFetcher = fetcherFactory.create(this::class.simpleName!!) {
        settings.apis
            .mapIndexed { index, api ->
                val accountId = settings.settings[index].accountId
                api.api.getClientInformation().awaitSingle().accounts.firstOrNull { it.id == accountId }!!
            }
    }

    private fun <T : Any?> T.logMissing(name: String, monoAccountId: String): T {
        if (this == null) {
            log.error { "Could not find alias for $name for Monobank accountId=$monoAccountId" }
        }
        return this
    }

    fun getMonoAccAlias(monoAccountId: String): String? = settings.byId[monoAccountId]
        ?.alias
        .logMissing("Monobank account", monoAccountId)

    suspend fun getAccounts() = monoAccountsFetcher.getData()

    suspend fun getAccountCurrency(monoAccountId: String): Currency? =
        monoAccountsFetcher.getData().firstOrNull { it.id == monoAccountId }?.currencyCode

    fun getTelegramChatIdAccByMono(monoAccountId: String) = settings.byId[monoAccountId]
        ?.telegramChatId
        .logMissing("Telegram chat ID", monoAccountId)

    fun getBudgetAccountId(monoAccountId: String): String? = settings.byId[monoAccountId]
        ?.budgetAccountId
        .logMissing("Budget account ID", monoAccountId)
}
