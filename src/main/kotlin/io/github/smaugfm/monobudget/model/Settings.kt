package io.github.smaugfm.monobudget.model

import com.charleskorn.kaml.Yaml
import io.github.smaugfm.monobudget.api.MonoApi
import io.github.smaugfm.monobudget.model.mcc.MccGroupType
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path

private val log = KotlinLogging.logger {}

@Serializable
data class Settings(
    val budgetBackend: BudgetBackend,
    val mono: MultipleMonoSettings,
    val bot: TelegramBotSettings,
    val mcc: MccOverride
) {
    companion object {
        fun load(path: Path): Settings = load(File(path.toString()).readText())

        internal fun load(content: String) = Yaml.default.decodeFromString<Settings>(content)
            .also {
                log.debug { "Loaded settings: $it" }
            }
    }

    @Serializable
    data class MultipleMonoSettings(
        val settings: List<MonoSettings>
    ) {
        @Transient
        val apis = settings.map { it.token }.map(::MonoApi)

        @Transient
        val byId = settings.associateBy { it.accountId }

        @Transient
        val monoAccountsIds = settings.map { it.accountId }

        @Transient
        val telegramChatIds = settings.map { it.telegramChatId }
    }

    @Serializable
    data class MonoSettings(
        val accountId: String,
        val token: String,
        val alias: String,
        val budgetAccountId: String,
        val telegramChatId: Long
    )

    @Serializable
    data class TelegramBotSettings(
        val token: String,
        val username: String
    )

    @Serializable
    data class MccOverride(
        val mccGroupToCategoryName: Map<MccGroupType, String> = emptyMap(),
        val mccToCategoryName: Map<Int, String> = emptyMap()
    )
}
