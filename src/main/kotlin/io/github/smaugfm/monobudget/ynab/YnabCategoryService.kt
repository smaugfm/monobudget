package io.github.smaugfm.monobudget.ynab

import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.financial.Amount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.koin.core.annotation.Single
import java.util.Currency

@Single(createdAtStart = true)
class YnabCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: YnabApi,
    scope: CoroutineScope,
    private val ynabBudgetBackend: BudgetBackend.YNAB
) : CategoryService() {

    private val categoriesFetcher = periodicFetcherFactory.create("YNAB categoires") {
        api.getCategoryGroups().flatMap {
            it.categories
        }
    }
    private val budgetCurrency = scope.async {
        api.getBudget(ynabBudgetBackend.ynabBudgetId)
            .currencyFormat
            .isoCode
            .let(Currency::getInstance)
    }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.getData().map {
            it.id to it.name
        }

    override suspend fun budgetedCategoryById(categoryId: String?): BudgetedCategory? {
        val category = categoriesFetcher.getData()
            .takeIf { categoryId != null }
            ?.find { it.id == categoryId }
            ?: return null

        return BudgetedCategory(
            category.name,
            BudgetedCategory.CategoryBudget(
                Amount.fromYnabAmount(category.balance),
                Amount.fromYnabAmount(category.budgeted),
                budgetCurrency.await()
            )
        )
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
}
