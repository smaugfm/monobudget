package com.github.smaugfm.util

import com.github.smaugfm.models.mcc.MccEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object MCC {
    val map: Map<Int, MccEntry> =
        Json.decodeFromString<List<MccEntry>>(resourceAsString("mcc.json")).associateBy { it.mcc }
}
