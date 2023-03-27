package io.github.smaugfm.monobudget.components.suggestion

import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.MCC
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

abstract class CategorySuggestionService(
    private val mccOverride: Settings.MccOverride
) {
    abstract suspend fun categoryIdByName(categoryName: String): String?
    abstract suspend fun categoryNameById(categoryId: String?): String?
    abstract suspend fun categoryIdToNameList(): List<Pair<String, String>>

    fun categoryNameByMcc(mcc: Int): String? = mccOverride.mccToCategoryName[mcc] ?: categoryNameByMccGroup(mcc)

    suspend fun categoryIdByMcc(mcc: Int): String? = categoryNameByMcc(mcc)?.let { categoryIdByName(it) }

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
}
