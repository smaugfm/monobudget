package io.github.smaugfm.monobudget.model.mcc

import kotlinx.serialization.Serializable

@Serializable
data class MccGroup(
    val type: MccGroupType,
    val description: String
)
