@file:UseSerializers(HashBiMapAsMapSerializer::class)

package com.github.smaugfm.settings

import com.github.smaugfm.serializers.HashBiMapAsMapSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

@Serializable
data class Settings(
    val ynabToken: String,
    val monoTokens: List<String>,
    val telegramBotToken: String,
    val telegramBotUsername: String,
    val ynabBudgetId: String,
    val mappings: Mappings,
) {
    companion object {
        fun load(path: Path) = Json.decodeFromString<Settings>(File(path.toString()).readText())
        fun loadDefault() = load(Paths.get("settings.json"))
    }

    @Suppress("unused")
    fun save(path: Path) {
        File(path.toString()).writeText(Json { prettyPrint = true }.encodeToString(serializer(), this))
    }
}
