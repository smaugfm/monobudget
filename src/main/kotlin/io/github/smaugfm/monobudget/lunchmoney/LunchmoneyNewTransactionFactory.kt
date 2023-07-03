package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.common.util.toLocalDateTime
import kotlinx.datetime.toJavaLocalDate
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.util.Currency

private val log = KotlinLogging.logger {}

@Single
class LunchmoneyNewTransactionFactory(
    private val accountsService: AccountsService
) : NewTransactionFactory<LunchmoneyInsertTransaction>() {

    override suspend fun create(statement: StatementItem): LunchmoneyInsertTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val categoryId = getCategoryId(statement)?.toLong()
        val accountCurrency =
            accountsService.getAccountCurrency(statement.accountId)!!

        return with(statement) {
            LunchmoneyInsertTransaction(
                date = time.toLocalDateTime().date.toJavaLocalDate(),
                amount = amount.toLunchmoneyAmount(accountCurrency),
                categoryId = categoryId,
                payee = description,
                currency = accountCurrency,
                assetId = getBudgetAccountId(statement).toLong(),
                recurringId = null,
                notes = getNotes(accountCurrency),
                status = getStatus(categoryId),
                externalId = id,
                tags = null
            )
        }
    }

    private fun getStatus(categoryId: Long?) = if (categoryId != null) {
        LunchmoneyTransactionStatus.CLEARED
    } else {
        LunchmoneyTransactionStatus.UNCLEARED
    }

    private fun StatementItem.getNotes(accountCurrency: Currency): String {
        val desc = "$mcc " + formatDescription()
        if (accountCurrency == currency) {
            return desc
        }

        return "${formatAmountWithCurrency()} $desc"
    }
}
