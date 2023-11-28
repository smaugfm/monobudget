package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.util.misc.PeriodicFetcherFactory
import org.koin.core.annotation.Single
import java.util.Currency

@Single(createdAtStart = true)
class YnabCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: YnabApi,
    private val ynab: BudgetBackend.YNAB,
) : CategoryService() {
    private val categoriesFetcher =
        periodicFetcherFactory.create("YNAB categories") {
            api.getCategoryGroups().flatMap {
                it.categories
            }
        }

    private val budgetCurrencyFetcher =
        periodicFetcherFactory.create("YNAB budget summary") {
            Currency.getInstance(api.getBudget(ynab.ynabBudgetId).currencyFormat.isoCode)
        }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.fetched().map {
            it.id to it.name
        }

    override suspend fun budgetedCategoryByIdInternal(categoryId: String): BudgetedCategory? {
        val category = categoriesFetcher.fetched().find { it.id == categoryId } ?: return null
        val currency = budgetCurrencyFetcher.fetched()

        return BudgetedCategory(
            category.name,
            if (category.budgeted > 0) {
                BudgetedCategory.CategoryBudget(
                    Amount.fromYnabAmount(category.balance, currency),
                    Amount.fromYnabAmount(category.budgeted, currency),
                )
            } else {
                null
            },
        )
    }

    override suspend fun categoryIdByName(categoryName: String): String? =
        categoriesFetcher.fetched()
            .firstOrNull { it.name == categoryName }
            ?.id
}
