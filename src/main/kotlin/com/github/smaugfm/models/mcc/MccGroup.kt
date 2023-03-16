package com.github.smaugfm.models.mcc

import kotlinx.serialization.Serializable

@Serializable
data class MccGroup(
    val type: MccGroupType,
    val description: String,
)

