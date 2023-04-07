package io.github.smaugfm.monobudget.common.model.settings

import io.github.smaugfm.monobudget.common.model.mcc.MccGroupType
import kotlinx.serialization.Serializable

@Serializable
data class MccOverrideSettings(
    val mccGroupToCategoryName: Map<MccGroupType, String> = emptyMap(),
    val mccToCategoryName: Map<Int, String> = emptyMap()
)
