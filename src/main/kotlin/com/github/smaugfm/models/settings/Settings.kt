@file:UseSerializers(HashBiMapAsMapSerializer::class)

package com.github.smaugfm.models.settings

import com.github.smaugfm.models.serializers.HashBiMapAsMapSerializer
import com.github.smaugfm.util.makeJson
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

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
        fun load(path: Path): Settings =
            load(File(path.toString()).readText())

        fun load(content: String) =
            Json.decodeFromString<Settings>(content).also {
                logger.debug { "Loaded settings: $it" }
            }
    }

    @Suppress("unused")
    fun save(path: Path) {
        File(path.toString()).writeText(json.encodeToString(serializer(), this))
    }
}

@Contextual
private val json = makeJson()
