package com.github.smaugfm.service.transaction

import com.github.smaugfm.models.settings.Settings

class CategorySuggestingService(
    private val settings: Settings
) {
    fun suggestByMcc(mcc: Int): String? =
        settings.mappings.mccToCategory[mcc]
}
