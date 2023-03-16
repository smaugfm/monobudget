package com.github.smaugfm.models.mcc

import kotlinx.serialization.Serializable

@Serializable
data class MccEntry(
    val mcc: Int,
    val gorup: MccGroup,
    val shortDescription: String,
    val fullDescription: String
)
