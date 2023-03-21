package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.util.MCC
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

abstract class CategorySuggestingService(
    private val mccOverride: Settings.MccOverride,
) {
    protected abstract suspend fun categoryIdByName(categoryName: String): String?

    suspend fun mapNameToCategoryId(mcc: Int): String? {
        val mccObj = MCC.map[mcc]
        if (mccObj == null)
            logger.warn { "Unknown MCC code $mcc" }
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
