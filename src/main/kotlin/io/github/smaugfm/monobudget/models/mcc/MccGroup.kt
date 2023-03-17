package io.github.smaugfm.monobudget.models.mcc

import kotlinx.serialization.Serializable

@Serializable
data class MccGroup(
    val type: MccGroupType,
    val description: String
)
