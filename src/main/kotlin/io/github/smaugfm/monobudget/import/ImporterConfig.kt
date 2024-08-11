package io.github.smaugfm.monobudget.import

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Serializable
data class ImporterConfig(
    private val imports: Map<String, String>,
) {
    fun getImports() = imports.entries.map { ImporterAccountConfig(it.key, it.value) }

    companion object {
        fun load(path: Path): ImporterConfig = load(File(path.toString()).readText())

        private fun load(content: String): ImporterConfig =
            Yaml(configuration = YamlConfiguration(strictMode = false))
                .decodeFromString<ImporterConfig>(content)
                .also {
                    log.debug { "Loaded import-config: $it" }
                }
    }
}
