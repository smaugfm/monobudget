package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.model.financial.Account
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger { }

@Single(createdAtStart = true)
@OptIn(ExperimentalSerializationApi::class)
class MonoAccountsService(
    fetcherFactory: PeriodicFetcherFactory,
    private val settings: Settings.MultipleMonoSettings
) : AccountsService() {

    private val monoAccountsFetcher = fetcherFactory.create(this::class.simpleName!!) {
        settings.apis
            .mapIndexed { index, api ->
                val accountId = settings.settings[index].accountId
                val monoAccount = api.api.getClientInformation().awaitSingle()
                    .accounts.firstOrNull { it.id == accountId }!!
                val id = monoAccount.id
                Account(
                    id,
                    settings.byId[id]!!.alias,
                    monoAccount.balance,
                    monoAccount.currencyCode
                )
            }
    }

    override suspend fun getAccounts() = monoAccountsFetcher.getData()

    override fun getTelegramChatIdAccByMono(monoAccountId: String) = settings.byId[monoAccountId]
        ?.telegramChatId
        .logMissing("Telegram chat ID", monoAccountId)

    override fun getBudgetAccountId(monoAccountId: String): String? = settings.byId[monoAccountId]
        ?.budgetAccountId
        .logMissing("Budget account ID", monoAccountId)

    private fun <T : Any?> T.logMissing(name: String, monoAccountId: String): T {
        if (this == null) {
            log.error { "Could not find alias for $name for Monobank accountId=$monoAccountId" }
        }
        return this
    }
}
