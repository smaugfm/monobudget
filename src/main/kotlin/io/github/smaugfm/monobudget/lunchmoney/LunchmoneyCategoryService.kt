package io.github.smaugfm.monobudget.lunchmoney

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.model.financial.Amount
import kotlinx.coroutines.reactor.awaitSingle
import org.koin.core.annotation.Single
import java.time.LocalDate

@Single(createdAtStart = true)
class LunchmoneyCategoryService(
    periodicFetcherFactory: PeriodicFetcherFactory,
    private val api: LunchmoneyApi
) : CategoryService() {

    private val categoriesFetcher = periodicFetcherFactory.create("Lunchmoney categories") {
        api.getAllCategories().awaitSingle()
    }

    private val budgetsFetcher = periodicFetcherFactory.create("Lunchmoney budgets") {
        val now = LocalDate.now()
        val startOfMonth = now.withDayOfMonth(1)
        val endOfMonth = now.withDayOfMonth(
            now.month.length(now.isLeapYear)
        )
        api.getBudgetSummary(
            startOfMonth,
            endOfMonth,
            null
        ).awaitSingle()
    }

    override suspend fun categoryIdToNameList(): List<Pair<String, String>> =
        categoriesFetcher.getData().map {
            it.id.toString() to it.name
        }

    override suspend fun budgetedCategoryById(categoryId: String?): BudgetedCategory? {
        if (categoryId == null) {
            return null
        }
        val idLong = categoryId.toLong()
        val categoryName = categoriesFetcher.getData().find { it.id == idLong }?.name
        val budget =
            budgetsFetcher
                .getData()
                .firstOrNull { it.categoryId == idLong }
                ?.data?.values?.first()
                ?.takeIf { it.budgetToBase != null && it.budgetAmount != null && it.budgetCurrency != null }
                ?.takeUnless { it.isAutomated == true }
                ?.let {
                    BudgetedCategory.CategoryBudget(
                        Amount.fromLunchmoneyAmount(
                            it.budgetToBase!! - it.spendingToBase,
                            it.budgetCurrency!!
                        ),
                        Amount.fromLunchmoneyAmount(
                            it.budgetToBase!!,
                            it.budgetCurrency!!
                        ),
                        it.budgetCurrency!!
                    )
                }

        return categoryName?.let { BudgetedCategory(it, budget) }
    }

    override suspend fun categoryIdByName(categoryName: String): String? = categoriesFetcher.getData()
        .firstOrNull { it.name == categoryName }
        ?.id
        ?.toString()
}
