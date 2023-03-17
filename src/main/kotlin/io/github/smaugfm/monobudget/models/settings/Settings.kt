@file:UseSerializers(HashBiMapAsMapSerializer::class)

package io.github.smaugfm.monobudget.models.settings

import io.github.smaugfm.monobudget.models.serializer.HashBiMapAsMapSerializer
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
    val mappings: Mappings
) {
    companion object {
        fun load(path: Path): Settings = load(File(path.toString()).readText())

        fun load(content: String) = Json.decodeFromString<Settings>(content).also {
            logger.debug { "Loaded settings: $it" }
        }
    }
}
