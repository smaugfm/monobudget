package io.github.smaugfm.monobudget.util

import io.github.smaugfm.monobudget.models.mcc.MccEntry
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object MCC {
    val map: Map<Int, MccEntry> =
        Json.decodeFromString<List<MccEntry>>(resourceAsString("mcc.json")).associateBy { it.mcc }
}
