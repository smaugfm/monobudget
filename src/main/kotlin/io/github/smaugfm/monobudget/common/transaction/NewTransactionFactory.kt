package io.github.smaugfm.monobudget.common.transaction

import io.github.smaugfm.monobudget.common.account.AccountsService
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class NewTransactionFactory<TNewTransaction> : KoinComponent {

    private val accounts: AccountsService by inject()
    private val categorySuggestingService: CategorySuggestionService by inject()

    abstract suspend fun create(statement: StatementItem): TNewTransaction

    protected fun getBudgetAccountId(statement: StatementItem) = accounts.getBudgetAccountId(statement.accountId)
        ?: error("Could not find Budgeting app account for mono account ${statement.accountId}")

    protected suspend fun getCategoryId(statement: StatementItem) =
        categorySuggestingService.categoryIdByMcc(statement.mcc)

    companion object {
        @JvmStatic
        protected fun StatementItem?.formatDescription() = (this?.description ?: "").replaceNewLines()
    }
}
