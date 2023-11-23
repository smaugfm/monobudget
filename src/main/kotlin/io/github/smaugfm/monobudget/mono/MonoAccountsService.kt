package io.github.smaugfm.monobudget.mono

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.toMonoAccount
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger { }

@Single(createdAtStart = true)
class MonoAccountsService : BankAccountService(), KoinComponent {
    private val settings: MultipleAccountSettings by inject()
    private val accounts =
        settings.settings.map { it.toMonoAccount() }

    override suspend fun getAccounts() = accounts

    override fun getTelegramChatIdByAccountId(accountId: String) =
        settings.byId[accountId]
            ?.telegramChatId
            .logMissing("Telegram chat ID", accountId)

    override fun getBudgetAccountId(accountId: String): String? =
        settings.byId[accountId]
            ?.budgetAccountId
            .logMissing("Budget account ID", accountId)

    private fun <T : Any?> T.logMissing(
        name: String,
        accountId: String,
    ): T {
        if (this == null) {
            log.error { "Could not find alias for $name for accountId=$accountId" }
        }
        return this
    }
}
