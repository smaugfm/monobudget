package io.github.smaugfm.monobudget.common.transaction

import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class NewTransactionFactory<TNewTransaction> : KoinComponent {
    private val bankAccounts: BankAccountService by inject()
    private val categorySuggestingService: CategoryService by inject()

    abstract suspend fun create(statement: StatementItem): TNewTransaction

    protected fun getBudgetAccountId(statement: StatementItem) =
        bankAccounts.getBudgetAccountId(
            statement.accountId,
        )
            ?: error("Could not find Budgeting app account for mono account ${statement.accountId}")

    protected suspend fun getCategoryId(statement: StatementItem) =
        categorySuggestingService.inferCategoryIdByMcc(statement.mcc)

    companion object {
        @JvmStatic
        protected fun StatementItem?.formatDescription() = (this?.description ?: "").replaceNewLines()
    }
}
