package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferDetector
import io.github.smaugfm.monobudget.common.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
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
    telegramMessageSender: TelegramMessageSender,
) : StatementItemProcessor<YnabTransactionDetail, YnabSaveTransaction>(
        ctx,
        transactionFactory,
        bankAccounts,
        transferDetector,
        messageFormatter,
        telegramMessageSender,
    )
