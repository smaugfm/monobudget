@file:UseSerializers(HashBiMapAsMapSerializer::class)

package com.github.smaugfm.settings

import com.github.smaugfm.serializers.HashBiMapAsMapSerializer
import com.github.smaugfm.serializers.URIAsStringSerializer
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URI
import java.nio.file.Path

@Serializable
data class Settings(
    val ynabToken: String,
    val monoTokens: List<String>,
    val telegramToken: String,
    @Serializable(with = URIAsStringSerializer::class)
    val webhookURI: URI,
    val ynabBudgetId: String,
    @Serializable(with = HashBiMapAsMapSerializer::class)
    val mono2Ynab: BiMap<String, String> = HashBiMap(),
    val telegram2Mono: Map<Long, List<String>> = emptyMap(),
) {
    companion object {
        fun load(path: Path) = Json.decodeFromString<Settings>(File(path.toString()).readText())
    }

    @Suppress("unused")
    fun save(path: Path) {
        File(path.toString()).writeText(Json { prettyPrint = true }.encodeToString(serializer(), this))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Settings) return false

        if (ynabBudgetId != other.ynabBudgetId) return false
        if (mono2Ynab.toSortedMap() != other.mono2Ynab.toSortedMap()) return false
        if (telegram2Mono != other.telegram2Mono) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ynabBudgetId.hashCode()
        result = 31 * result + mono2Ynab.toSortedMap().hashCode()
        result = 31 * result + telegram2Mono.hashCode()
        return result
    }

}
