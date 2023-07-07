package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.financial.Amount
import org.koin.core.annotation.Single
import java.util.Currency

@Single(createdAtStart = true)
class YnabCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: YnabApi
) : CategoryService() {

    private val categoriesFetcher = periodicFetcherFactory.create("YNAB categoires") {
        api.getCategoryGroups().flatMap {
            it.categories
        }
    }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.getData().map {
            it.id to it.name
        }

    override suspend fun budgetedCategoryByIdInternal(
        categoryId: String,
        accountCurrency: Currency
    ): BudgetedCategory? {
        val category = categoriesFetcher.getData().find { it.id == categoryId } ?: return null

        return BudgetedCategory(
            category.name,
            if (category.budgeted > 0) {
                BudgetedCategory.CategoryBudget(
                    Amount.fromYnabAmount(category.balance, accountCurrency),
                    Amount.fromYnabAmount(category.budgeted, accountCurrency)
                )
            } else {
                null
            }
        )
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
}
