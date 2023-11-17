package io.github.smaugfm.monobudget.common.lifecycle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.pp
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

abstract class StatementItemProcessor<TTransaction, TNewTransaction> : KoinComponent {
    private val statementItem by inject<StatementItem>()
    private val transactionFactory by inject<TransactionFactory<TTransaction, TNewTransaction>>()
    private val bankAccounts by inject<BankAccountService>()
    private val transferDetector by inject<TransferBetweenAccountsDetector<TTransaction>>()
    private val messageFormatter by inject<TransactionMessageFormatter<TTransaction>>()
    private val telegramMessageSender by inject<TelegramMessageSender>()

    suspend fun process() {
        logStatement()
        processStatement()
    }

    private suspend fun processStatement() {
        val maybeTransfer =
            transferDetector.checkTransfer()

        val transaction = transactionFactory.create(maybeTransfer)
        val message = messageFormatter.format(transaction)

        telegramMessageSender.send(statementItem.accountId, message)
    }

    private suspend fun logStatement() {
        val alias = bankAccounts.getAccountAlias(statementItem.accountId)
        with(statementItem) {
            log.info {
                "Incoming transaction from $alias's account.\n" +
                    if (log.isTraceEnabled()) {
                        this.pp()
                    } else {
                        "\tAmount: ${amount}\n" +
                            "\tDescription: $description" +
                            "\tMemo: $comment"
                    }
            }
        }
    }
}
