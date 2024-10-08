package io.github.smaugfm.monobudget.common.statement.lifecycle

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.account.TransferDetector
import io.github.smaugfm.monobudget.common.notify.StatementItemNotificationSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.pp

private val log = KotlinLogging.logger {}

abstract class StatementItemProcessor<TTransaction, TNewTransaction>(
    private val ctx: StatementProcessingContext,
    private val transactionFactory: TransactionFactory<TTransaction, TNewTransaction>,
    private val bankAccounts: BankAccountService,
    private val transferDetector: TransferDetector<TTransaction>,
    private val messageFormatter: TransactionMessageFormatter<TTransaction>,
    private val notificationSender: StatementItemNotificationSender,
) {
    suspend fun process() {
        logStatement()
        processStatement()
    }

    private suspend fun processStatement() {
        val maybeTransfer =
            transferDetector.checkForTransfer()

        val transaction = transactionFactory.create(maybeTransfer)
        val message = messageFormatter.format(ctx.item, transaction)

        notificationSender.notify(ctx.item.accountId, message)
    }

    private suspend fun logStatement() {
        val alias = bankAccounts.getAccountAlias(ctx.item.accountId)
        with(ctx.item) {
            log.info {
                "Incoming transaction from $alias's account.\n" +
                    if (log.isTraceEnabled()) {
                        this.pp()
                    } else {
                        "\tAmount: ${amount}\n" +
                            "\tDescription: $description\n" +
                            "\tMemo: $comment"
                    }
            }
        }
    }
}
