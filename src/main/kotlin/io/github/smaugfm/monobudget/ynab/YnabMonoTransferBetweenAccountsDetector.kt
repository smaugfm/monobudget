package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import kotlinx.coroutines.Deferred
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import kotlin.time.Duration.Companion.minutes

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class YnabMonoTransferBetweenAccountsDetector(
    bankAccounts: BankAccountService,
    ctx: StatementProcessingContext
) : TransferBetweenAccountsDetector<YnabTransactionDetail>(
    bankAccounts,
    ctx,
    cache
) {
    companion object {
        private val cache =
            ConcurrentExpiringMap<StatementItem, Deferred<YnabTransactionDetail>>(1.minutes)
    }
}
