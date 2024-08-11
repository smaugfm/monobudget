package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferDetector
import io.github.smaugfm.monobudget.common.notify.StatementItemNotificationSender
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class YnabStatementItemProcessor(
    ctx: StatementProcessingContext,
    transactionFactory: TransactionFactory<YnabTransactionDetail, YnabSaveTransaction>,
    bankAccounts: BankAccountService,
    transferDetector: TransferDetector<YnabTransactionDetail>,
    messageFormatter: TransactionMessageFormatter<YnabTransactionDetail>,
    notificationSender: StatementItemNotificationSender,
) : StatementItemProcessor<YnabTransactionDetail, YnabSaveTransaction>(
        ctx,
        transactionFactory,
        bankAccounts,
        transferDetector,
        messageFormatter,
        notificationSender,
    )
