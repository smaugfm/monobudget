package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.MCC
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

sealed class CategorySuggestingService(
    private val mccOverride: Settings.MccOverride,
) {
    protected abstract suspend fun categoryIdByName(categoryName: String): String?

    abstract suspend fun categoryNameById(categoryId: String): String?

    suspend fun byMcc(mcc: Int): String? {
        val mccObj = MCC.map[mcc]
        if (mccObj == null)
            log.warn { "Unknown MCC code $mcc" }
        else {
            val categoryName = mccOverride.mccGroupToCategoryName[mccObj.group.type]
            if (categoryName != null)
                return categoryName
        }
        return mccOverride.mccToCategoryName[mcc]?.let {
            categoryIdByName(it)
        }
    }
}
