package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferDetector
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class YnabTransferDetector(
    bankAccounts: BankAccountService,
    ctx: StatementProcessingContext,
    cache: YnabTransferCache,
) : TransferDetector<YnabTransactionDetail>(
        bankAccounts,
        ctx,
        cache,
    )
