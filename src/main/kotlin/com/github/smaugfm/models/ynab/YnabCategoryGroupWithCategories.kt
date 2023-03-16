package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabCategoryGroupWithCategories(
    val id: String,
    val name: String,
    val hidden: Boolean,
    val deleted: Boolean,
    val categories: List<YnabCategory>,
)
