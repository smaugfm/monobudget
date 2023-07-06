package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyBudget
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.financial.Amount
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import java.time.LocalDate
import java.util.Currency

@Single(createdAtStart = true)
class LunchmoneyCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: LunchmoneyApi
) : CategoryService() {

    private val categoriesFetcher = periodicFetcherFactory.create("Lunchmoney categories") {
        api.getAllCategories().awaitSingle()
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
        ?.toString()

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.getData().map {
            it.id.toString() to it.name
        }

    override suspend fun budgetedCategoryById(categoryId: String?): BudgetedCategory? {
        val categoryIdLong = categoryId?.toLongOrNull() ?: return null
        val categoryName = categoriesFetcher.getData().find { it.id == categoryIdLong }?.name

        val budget = getCategoryBudget(categoryIdLong)

        return categoryName?.let { BudgetedCategory(it, budget) }
    }

    private suspend fun fetchCurrentBudget(categoryId: Long): LunchmoneyBudget? {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(
            now.month.length(now.isLeapYear)
        )
        val budgets = api.getBudgetSummary(
            startOfMonth,
            endOfMonth,
            null
        ).awaitSingle()

        return budgets.firstOrNull { it.categoryId != null && it.categoryId == categoryId }
    }

    private suspend fun getCategoryBudget(categoryIdLong: Long): BudgetedCategory.CategoryBudget? =
        fetchCurrentBudget(categoryIdLong)
            ?.data?.values?.firstOrNull { it.isAutomated != true }
            ?.let {
                val budget = it.budgetToBase ?: return@let null
                val spending = it.spendingToBase ?: return@let null
                val currency = it.budgetCurrency ?: return@let null
                toCategoryBudget(budget, spending, currency)
            }

    private fun toCategoryBudget(budget: Double, spending: Double, currency: Currency) =
        BudgetedCategory.CategoryBudget(
            Amount.fromLunchmoneyAmount(budget - spending, currency),
            Amount.fromLunchmoneyAmount(budget, currency),
            currency
        )
}
