package io.github.smaugfm.monobudget.common.category

import io.github.smaugfm.monobudget.common.misc.MCC
import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.model.settings.MccOverrideSettings
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger { }

abstract class CategoryService : KoinComponent {
    private val mccOverride: MccOverrideSettings by inject()

    abstract suspend fun budgetedCategoryByIdInternal(
        categoryId: String
    ): BudgetedCategory?

    suspend fun budgetedCategoryById(categoryId: String?): BudgetedCategory? {
        if (categoryId == null) {
            return null
        }

        return budgetedCategoryByIdInternal(categoryId)
    }

    abstract suspend fun categoryIdToNameList(): List<Pair<String, String>>

    suspend fun inferCategoryIdByMcc(mcc: Int): String? =
        inferCategoryNameByMcc(mcc)?.let { categoryIdByName(it) }

    protected abstract suspend fun categoryIdByName(categoryName: String): String?

    fun inferCategoryNameByMcc(mcc: Int): String? =
        mccOverride.mccToCategoryName[mcc] ?: categoryNameByMccGroup(mcc)

    private fun categoryNameByMccGroup(mcc: Int): String? {
        val mccObj = MCC.map[mcc]
        if (mccObj == null) {
            log.warn { "Unknown MCC code $mcc" }
        } else {
            val categoryName = mccOverride.mccGroupToCategoryName[mccObj.group.type]
            if (categoryName != null) {
                return categoryName
            }
        }
        return null
    }

    data class BudgetedCategory(
        val categoryName: String,
        val budget: CategoryBudget?
    ) {
        data class CategoryBudget(
            val left: Amount,
            val budgetedThisMonth: Amount
        )
    }
}
