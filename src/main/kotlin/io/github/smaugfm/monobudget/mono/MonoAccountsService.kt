package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.financial.Account
import io.github.smaugfm.monobudget.common.model.settings.MonoAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.OtherAccountSettings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.koin.core.annotation.Single
import reactor.core.publisher.Flux

private val log = KotlinLogging.logger { }

@Single(createdAtStart = true)
@OptIn(ExperimentalSerializationApi::class)
class MonoAccountsService(
    fetcherFactory: PeriodicFetcherFactory,
    private val settings: MultipleAccountSettings
) : BankAccountService() {

    private val otherAccounts = settings.settings.filterIsInstance<OtherAccountSettings>()
        .map { Account(it.accountId, it.alias, it.currency) }
    private val monoApis = settings.settings.filterIsInstance<MonoAccountSettings>()
        .map { MonoApi(it.token) to it.accountId }

    private val fetcher = fetcherFactory.create("Monobank accounts") {
        Flux.concat(
            monoApis.map { (api, accountId) ->
                api.api.getClientInformation()
                    .map { info -> info.accounts.first { a -> a.id == accountId } }
                    .map {
                        Account(
                            accountId,
                            settings.byId[accountId]!!.alias,
                            it!!.currencyCode
                        )
                    }
            }
        ).collectList().awaitSingle() + otherAccounts
    }

    override suspend fun getAccounts() = fetcher.getData().filter { it.id in settings.accountIds }

    override fun getTelegramChatIdByAccountId(accountId: String) = settings.byId[accountId]
        ?.telegramChatId
        .logMissing("Telegram chat ID", accountId)

    override fun getBudgetAccountId(accountId: String): String? = settings.byId[accountId]
        ?.budgetAccountId
        .logMissing("Budget account ID", accountId)

    private fun <T : Any?> T.logMissing(name: String, accountId: String): T {
        if (this == null) {
            log.error { "Could not find alias for $name for accountId=$accountId" }
        }
        return this
    }
}
