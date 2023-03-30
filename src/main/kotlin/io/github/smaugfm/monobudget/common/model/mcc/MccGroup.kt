package io.github.smaugfm.monobudget.common.model.mcc

import kotlinx.serialization.Serializable

@Serializable
data class MccGroup(
    val type: MccGroupType,
    val description: String
)
