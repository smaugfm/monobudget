package io.github.smaugfm.monobudget.components.transaction.factory

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.util.replaceNewLines

abstract class NewTransactionFactory<TNewTransaction>(
    private val monoAccountsService: MonoAccountsService,
    private val categorySuggestingService: CategorySuggestionService
) {
    abstract suspend fun create(response: MonoWebhookResponseData): TNewTransaction

    protected fun getBudgetAccountId(response: MonoWebhookResponseData) =
        monoAccountsService.getBudgetAccountId(response.account)
            ?: error("Could not find Budgeting app account for mono account ${response.account}")

    protected suspend fun getCategoryId(response: MonoWebhookResponseData) =
        categorySuggestingService.categoryIdByMcc(response.statementItem.mcc)

    companion object {
        @JvmStatic
        protected fun MonoStatementItem?.formatDescription() = (this?.description ?: "").replaceNewLines()
    }
}
