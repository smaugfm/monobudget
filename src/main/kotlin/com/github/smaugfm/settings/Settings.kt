package com.github.smaugfm.settings

import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class Settings(
    val ynabBudgetIt: String,
    val mono2Ynab: Map<String, String> = emptyMap()
) {
    companion object {
        fun load(path: Path) = Json.decodeFromString<Settings>(File(path.toString()).readText())
    }

    @Suppress("unused")
    fun save(path: Path) {
        File(path.toString()).writeText(Json { prettyPrint = true }.encodeToString(serializer(), this))
    }
}
