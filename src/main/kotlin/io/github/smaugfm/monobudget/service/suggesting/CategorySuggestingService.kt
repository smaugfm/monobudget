package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.models.Settings

abstract class CategorySuggestingService(
    private val mccOverride: Settings.MccOverride,
) {
    protected abstract suspend fun categoryIdByName(categoryName: String): String?
    suspend fun mapNameToCategoryId(mcc: Int): String? =
        mccOverride.mccToCategoryName[mcc]?.let {
            categoryIdByName(it)
        }
}
