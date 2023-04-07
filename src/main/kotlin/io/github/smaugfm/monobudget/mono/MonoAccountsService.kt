package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.financial.Account
import io.github.smaugfm.monobudget.common.model.settings.MonoAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.OtherAccountSettings
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger { }

@Single(createdAtStart = true)
@OptIn(ExperimentalSerializationApi::class)
class MonoAccountsService(
    fetcherFactory: PeriodicFetcherFactory,
    private val settings: MultipleAccountSettings
) : AccountsService() {

    private val fetcher = fetcherFactory.create(this::class.simpleName!!) {
        settings.settings.map {
            when (it) {
                is OtherAccountSettings ->
                    Account(it.accountId, it.alias, it.currency)

                is MonoAccountSettings -> {
                    val api = MonoApi(it.token)

                    val accountId = it.accountId
                    val monoAccount =
                        api.api.getClientInformation().awaitSingle()
                            .accounts.firstOrNull { account -> account.id == accountId }!!
                    Account(
                        accountId,
                        settings.byId[accountId]!!.alias,
                        monoAccount.currencyCode
                    )
                }
            }
        }
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
