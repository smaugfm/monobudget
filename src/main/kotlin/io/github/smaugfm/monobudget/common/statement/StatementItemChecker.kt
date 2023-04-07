package io.github.smaugfm.monobudget.common.statement

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.misc.SimpleCache
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.model.settings.MultipleAccountSettings
import io.github.smaugfm.monobudget.common.util.formatAmount
import io.github.smaugfm.monobudget.common.util.pp
import mu.KotlinLogging
import org.koin.core.annotation.Single

private val log = KotlinLogging.logger {}

@Single
class StatementItemChecker(
    private val accounts: AccountsService,
    private val accountSettings: MultipleAccountSettings
) {
    private val idsCache = SimpleCache<String, Unit> {}

    suspend fun check(item: StatementItem): Boolean {
        if (!checkValid(item) || !checkDuplicate(item)) {
            return false
        }

        logStatement(item)

        return true
    }

    private fun checkDuplicate(item: StatementItem): Boolean {
        if (!idsCache.checkAndPutKey(item.id, Unit)) {
            log.info { "Duplicate statement $item" }
            return false
        }

        return true
    }

    private fun checkValid(item: StatementItem): Boolean {
        if (!accountSettings.accountIds.contains(item.accountId)) {
            log.info {
                "Skipping transaction from Monobank " +
                    "accountId=${item.accountId} because this account is not configured."
            }
            return false
        }

        return true
    }

    private suspend fun logStatement(item: StatementItem) {
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
}
