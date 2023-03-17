package io.github.smaugfm.monobudget.service.transaction

import io.github.smaugfm.monobudget.models.settings.Settings

class CategorySuggestingService(
    private val settings: Settings
) {
    fun suggestByMcc(mcc: Int): String? = settings.mappings.mccToCategory[mcc]
}
