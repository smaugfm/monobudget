package io.github.smaugfm.monobudget.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabCategoryGroupsWithCategoriesWrapper(
    val categoryGroups: List<YnabCategoryGroupWithCategories>,
    val serverKnowledge: Long
)
