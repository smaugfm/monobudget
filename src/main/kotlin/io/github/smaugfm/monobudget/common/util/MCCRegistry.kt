package io.github.smaugfm.monobudget.common.util

import io.github.smaugfm.monobudget.common.model.mcc.MccEntry
import kotlinx.serialization.json.Json

object MCCRegistry {
    val map: Map<Int, MccEntry> =
        Json.decodeFromString<List<MccEntry>>(resourceAsString("mcc.json")).associateBy { it.mcc }
}
