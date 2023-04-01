package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter.Companion.formatAmountWithCurrency
import io.github.smaugfm.monobudget.common.util.toLocalDateTime
import kotlinx.datetime.toJavaLocalDate
import mu.KotlinLogging
import org.koin.core.annotation.Single
import org.koin.core.component.inject
import java.util.Currency

private val log = KotlinLogging.logger {}

@Single
class LunchmoneyNewTransactionFactory : NewTransactionFactory<LunchmoneyInsertTransaction>() {
    private val monoAccountsService: MonoAccountsService by inject()

    override suspend fun create(response: MonoWebhookResponseData): LunchmoneyInsertTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val categoryId = getCategoryId(response)?.toLong()
        val accountCurrency =
            monoAccountsService.getAccountCurrency(response.account)!!

        return with(response.statementItem) {
            LunchmoneyInsertTransaction(
                date = time.toLocalDateTime().date.toJavaLocalDate(),
                amount = getAmount(accountCurrency),
                categoryId = categoryId,
                payee = description,
                currency = accountCurrency,
                assetId = getBudgetAccountId(response).toLong(),
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

    private fun MonoStatementItem.getNotes(accountCurrency: Currency): String {
        val desc = "$mcc " + formatDescription()
        if (accountCurrency == currencyCode) {
            return desc
        }

        return "${formatAmountWithCurrency(operationAmount, currencyCode)} $desc"
    }

    companion object {
        private fun MonoStatementItem.getAmount(currency: Currency) = lunchmoneyAmount(amount, currency)

        fun lunchmoneyAmount(amount: Long, currency: Currency) = amount.toBigDecimal().setScale(2) /
            (10.toBigDecimal().pow(currency.defaultFractionDigits))
    }
}
