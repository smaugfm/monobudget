package io.github.smaugfm.monobudget.common.model.settings

import com.charleskorn.kaml.Yaml
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.io.File
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Serializable
data class Settings(
    val budgetBackend: BudgetBackend,
    val accounts: MultipleAccountSettings,
    val bot: TelegramBotSettings,
    val mcc: MccOverrideSettings = MccOverrideSettings(),
    val transfer: List<OtherBanksTransferSettings> = emptyList()
) {
    companion object {
        fun load(path: Path): Settings = load(File(path.toString()).readText())

        internal fun load(content: String) =
            Yaml.default.decodeFromString<Settings>(content)
                .also {
                    log.debug { "Loaded settings: $it" }
                }
    }
}
