package io.github.smaugfm.monobudget.common.transaction

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.CategorySuggestionService
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class NewTransactionFactory<TNewTransaction> : KoinComponent {

    private val monoAccountsService: MonoAccountsService by inject()
    private val categorySuggestingService: CategorySuggestionService by inject()

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
