package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class LunchmoneyStatementItemProcessor(
    statementItem: StatementItem,
    transactionFactory: TransactionFactory<LunchmoneyTransaction, LunchmoneyInsertTransaction>,
    bankAccounts: BankAccountService,
    transferDetector: TransferBetweenAccountsDetector<LunchmoneyTransaction>,
    messageFormatter: TransactionMessageFormatter<LunchmoneyTransaction>,
    telegramMessageSender: TelegramMessageSender
) : StatementItemProcessor<LunchmoneyTransaction, LunchmoneyInsertTransaction>(
    statementItem,
    transactionFactory,
    bankAccounts,
    transferDetector,
    messageFormatter,
    telegramMessageSender
)
