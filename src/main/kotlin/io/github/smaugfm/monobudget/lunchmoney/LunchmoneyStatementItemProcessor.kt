package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferDetector
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class LunchmoneyStatementItemProcessor(
    ctx: StatementProcessingContext,
    transactionFactory: TransactionFactory<LunchmoneyTransaction, LunchmoneyInsertTransaction>,
    bankAccounts: BankAccountService,
    transferDetector: TransferDetector<LunchmoneyTransaction>,
    messageFormatter: TransactionMessageFormatter<LunchmoneyTransaction>,
    telegramMessageSender: TelegramMessageSender,
) : StatementItemProcessor<LunchmoneyTransaction, LunchmoneyInsertTransaction>(
        ctx,
        transactionFactory,
        bankAccounts,
        transferDetector,
        messageFormatter,
        telegramMessageSender,
    )
