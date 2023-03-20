package io.github.smaugfm.monobudget.service.suggesting

import io.github.smaugfm.monobudget.models.Settings

class MccCategorySuggestingService(
    private val mccOverride: Settings.MccOverride
) {
    fun suggestCategoryNameByMcc(mcc: Int): String? =
        mccOverride.mccToCategoryName[mcc]
}
