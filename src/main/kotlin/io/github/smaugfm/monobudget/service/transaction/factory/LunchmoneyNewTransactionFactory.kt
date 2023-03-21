package io.github.smaugfm.monobudget.service.transaction.factory

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import io.github.smaugfm.monobudget.util.toLocalDateTime
import kotlinx.datetime.toJavaLocalDate
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class LunchmoneyNewTransactionFactory(
    monoAccountsService: MonoAccountsService,
    categorySuggestingService: CategorySuggestingService,
) : NewTransactionFactory<LunchmoneyInsertOrUpdateTransaction>(monoAccountsService, categorySuggestingService) {
    override suspend fun create(response: MonoWebhookResponseData): LunchmoneyInsertOrUpdateTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val categoryId = getCategoryId(response)?.toLong()

        return with(response.statementItem) {
            LunchmoneyInsertOrUpdateTransaction(
                date = time.toLocalDateTime().date.toJavaLocalDate(),
                amount = amount.toBigDecimal() / response.statementItem.currencyCode.defaultFractionDigits.toBigDecimal(),
                categoryId = categoryId,
                payee = description,
                currency = currencyCode,
                assetId = getBudgetAccountId(response).toLong(),
                recurringId = null,
                notes = "$mcc " + formatDescription(),
                status = if (categoryId != null) LunchmoneyTransactionStatus.CLEARED else LunchmoneyTransactionStatus.UNCLEARED,
                externalId = null,
                tags = null
            )
        }
    }
}
