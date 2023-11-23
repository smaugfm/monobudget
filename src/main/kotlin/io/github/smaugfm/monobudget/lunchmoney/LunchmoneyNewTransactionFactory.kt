package io.github.smaugfm.monobudget.lunchmoney

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.common.util.toLocalDateTime
import kotlinx.datetime.toJavaLocalDate
import org.koin.core.annotation.Single
import java.util.Currency

private val log = KotlinLogging.logger {}

@Single
class LunchmoneyNewTransactionFactory : NewTransactionFactory<LunchmoneyInsertTransaction>() {
    override suspend fun create(statement: StatementItem): LunchmoneyInsertTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val categoryId = getCategoryId(statement)?.toLong()

        return with(statement) {
            LunchmoneyInsertTransaction(
                date = time.toLocalDateTime().date.toJavaLocalDate(),
                amount = amount.toLunchmoneyAmountBigDecimal(),
                categoryId = categoryId,
                payee = description,
                currency = amount.currency,
                assetId = getBudgetAccountId(statement).toLong(),
                recurringId = null,
                notes = getNotes(amount.currency),
                status = getStatus(categoryId),
                externalId = id,
                tags = null,
            )
        }
    }

    private fun getStatus(categoryId: Long?) =
        if (categoryId != null) {
            LunchmoneyTransactionStatus.CLEARED
        } else {
            LunchmoneyTransactionStatus.UNCLEARED
        }

    private fun StatementItem.getNotes(accountCurrency: Currency): String {
        val desc = "$mcc " + formatDescription()
        if (accountCurrency == currency) {
            return desc
        }

        return "${formatAmount()} $desc"
    }
}
