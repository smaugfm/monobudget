package io.github.smaugfm.monobudget.common.misc

import io.github.smaugfm.monobudget.common.model.mcc.MccEntry
import io.github.smaugfm.monobudget.common.util.resourceAsString
import kotlinx.serialization.json.Json

object MCC {
    val map: Map<Int, MccEntry> =
        Json.decodeFromString<List<MccEntry>>(resourceAsString("mcc.json")).associateBy { it.mcc }
}
