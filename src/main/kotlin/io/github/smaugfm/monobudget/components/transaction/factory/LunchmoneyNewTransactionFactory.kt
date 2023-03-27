package io.github.smaugfm.monobudget.components.transaction.factory

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.util.toLocalDateTime
import kotlinx.datetime.toJavaLocalDate
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class LunchmoneyNewTransactionFactory :
    NewTransactionFactory<LunchmoneyInsertTransaction>() {
    override suspend fun create(response: MonoWebhookResponseData): LunchmoneyInsertTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val categoryId = getCategoryId(response)?.toLong()

        return with(response.statementItem) {
            LunchmoneyInsertTransaction(
                date = time.toLocalDateTime().date.toJavaLocalDate(),
                amount = lunchmoneyAmount(),
                categoryId = categoryId,
                payee = description,
                currency = currencyCode,
                assetId = getBudgetAccountId(response).toLong(),
                recurringId = null,
                notes = "$mcc " + formatDescription(),
                status = if (categoryId != null) {
                    LunchmoneyTransactionStatus.CLEARED
                } else {
                    LunchmoneyTransactionStatus.UNCLEARED
                },
                externalId = null,
                tags = null
            )
        }
    }

    private fun MonoStatementItem.lunchmoneyAmount() =
        amount.toBigDecimal() / 10.toBigDecimal().pow(currencyCode.defaultFractionDigits)
}
