package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.misc.ConcurrentExpiringMap
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import kotlinx.coroutines.Deferred
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import kotlin.time.Duration.Companion.minutes

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class LunchmoneyMonoTransferBetweenAccountsDetector(
    bankAccounts: BankAccountService,
    ctx: StatementProcessingContext
) : TransferBetweenAccountsDetector<LunchmoneyTransaction>(bankAccounts, ctx, cache) {
    companion object {
        private val cache =
            ConcurrentExpiringMap<StatementItem, Deferred<LunchmoneyTransaction>>(1.minutes)
    }
}
