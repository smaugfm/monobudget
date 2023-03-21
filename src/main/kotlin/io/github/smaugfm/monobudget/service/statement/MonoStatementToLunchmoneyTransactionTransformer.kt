package io.github.smaugfm.monobudget.service.statement

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import io.github.smaugfm.monobudget.util.replaceNewLines
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class MonoStatementToLunchmoneyTransactionTransformer(
    private val monoAccountsService: MonoAccountsService,
    private val categorySuggestingService: CategorySuggestingService,
) {
    suspend fun transform(response: MonoWebhookResponseData): LunchmoneyInsertOrUpdateTransaction {
        log.debug { "Transforming Monobank statement to Lunchmoney transaction." }

        val accountId = monoAccountsService.getBudgetAccountId(response.account)?.toLong()
            ?: error("Could not find Lunchmoney account for mono account ${response.account}")

        val categoryId = categorySuggestingService.mapNameToCategoryId(response.statementItem.mcc)?.toLong()

        return with(response.statementItem) {
            LunchmoneyInsertOrUpdateTransaction(
                date = time.toLocalDateTime(TimeZone.currentSystemDefault()).date.toJavaLocalDate(),
                amount = amount.toBigDecimal() / response.statementItem.currencyCode.defaultFractionDigits.toBigDecimal(),
                categoryId = categoryId,
                payee = description,
                currency = currencyCode,
                assetId = accountId,
                recurringId = null,
                notes = "$mcc " + (response.statementItem.description ?: "").replaceNewLines(),
                status = if (categoryId != null) LunchmoneyTransactionStatus.CLEARED else LunchmoneyTransactionStatus.UNCLEARED,
                externalId = null,
                tags = null
            )
        }
    }
}
