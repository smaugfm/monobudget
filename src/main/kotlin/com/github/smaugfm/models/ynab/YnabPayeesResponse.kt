package com.github.smaugfm.models.ynab

import kotlinx.serialization.Serializable

@Serializable
data class YnabPayeesResponse(
    val data: YnabPayeesWrapper,
)
