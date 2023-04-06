package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.StatementItemChecker
import io.github.smaugfm.monobudget.common.util.formatAmount
import io.github.smaugfm.monobudget.common.util.pp
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class MonoWebhookResponseChecker(
    private val accounts: AccountsService,
    private val monoSettings: Settings.MultipleMonoSettings
) : StatementItemChecker() {

    override suspend fun check(item: StatementItem): Boolean {
        if (item !is MonobankWebhookResponseStatementItem)
            return true

        if (!checkValid(item) && super.check(item)) {
            return false
        }

        logItem(item)

        return true
    }

    private suspend fun logItem(item: StatementItem) {
        with(item) {
            val alias = accounts.getAccountAlias(accountId)
            log.info {
                "Incoming transaction from $alias's account.\n" +
                    if (log.isDebugEnabled) {
                        this.pp()
                    } else {
                        "\tAmount: ${currency.formatAmount(amount)}\n" +
                            "\tDescription: $description" +
                            "\tMemo: $comment"
                    }
            }
        }
    }

    private fun checkValid(item: StatementItem): Boolean {
        if (!monoSettings.monoAccountsIds.contains(item.accountId)) {
            log.info {
                "Skipping transaction from Monobank " +
                    "accountId=${item.accountId} because this account is not configured."
            }
            return false
        }

        return true
    }
}
