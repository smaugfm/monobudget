package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyBudget
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.util.misc.PeriodicFetcherFactory
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import java.time.LocalDate
import java.util.Currency

@Single(createdAtStart = true)
class LunchmoneyCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: LunchmoneyApi,
) : CategoryService() {
    private val categoriesFetcher =
        periodicFetcherFactory.create("Lunchmoney categories") {
            api.getAllCategories().awaitSingle()
        }

    override suspend fun categoryIdByName(categoryName: String): String? =
        categoriesFetcher.fetched()
            .firstOrNull { it.name == categoryName }
            ?.id
            ?.toString()

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.fetched().map {
            it.id.toString() to it.name
        }

    override suspend fun budgetedCategoryByIdInternal(categoryId: String): BudgetedCategory? {
        val categoryIdLong = categoryId.toLong()
        val categoryName = categoriesFetcher.fetched().find { it.id == categoryIdLong }?.name ?: return null

        val budget = getCategoryBudget(categoryIdLong)

        return BudgetedCategory(categoryName, budget)
    }

    private suspend fun getCategoryBudget(categoryIdLong: Long): BudgetedCategory.CategoryBudget? =
        fetchCurrentBudget(categoryIdLong)
            ?.data?.values?.firstOrNull { it.isAutomated != true }
            ?.let {
                val budgeted = it.budgetToBase ?: return@let null
                val spending = it.spendingToBase ?: return@let null
                val currency = it.budgetCurrency ?: return@let null
                if (budgeted <= 0) return@let null

                toCategoryBudget(budgeted, spending, currency)
            }

    private suspend fun fetchCurrentBudget(categoryId: Long): LunchmoneyBudget? {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth =
            now.withDayOfMonth(
                now.month.length(now.isLeapYear),
            )
        val budgets =
            api.getBudgetSummary(
                startOfMonth,
                endOfMonth,
                null,
            ).awaitSingle()

        return budgets.firstOrNull { categoryId == it.categoryId }
    }

    private fun toCategoryBudget(
        budget: Double,
        spending: Double,
        currency: Currency,
    ) = BudgetedCategory.CategoryBudget(
        Amount.fromLunchmoneyAmount(budget - spending, currency),
        Amount.fromLunchmoneyAmount(budget, currency),
    )
}
