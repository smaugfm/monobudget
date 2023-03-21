package io.github.smaugfm.monobudget.service.statement

import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertOrUpdateTransaction
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

class MonoStatementToLunchmoneyTransactionTransformer(
    private val monoAccountsService: MonoAccountsService,
    private val categorySuggestingService: CategorySuggestingService,
) {
    fun transform(response: MonoWebhookResponseData): LunchmoneyInsertOrUpdateTransaction {
        TODO(response.toString())
    }
}
