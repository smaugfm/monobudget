package io.github.smaugfm.monobudget.model.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabCategoryGroupsWithCategoriesWrapper(
    val categoryGroups: List<YnabCategoryGroupWithCategories>
)
