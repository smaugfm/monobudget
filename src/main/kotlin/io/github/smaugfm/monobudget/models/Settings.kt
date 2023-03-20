package io.github.smaugfm.monobudget.models

import io.github.smaugfm.monobudget.api.MonoApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.File
import java.nio.file.Path

private val logger = KotlinLogging.logger {}

@Serializable
data class Settings(
    val budgetBackend: BudgetBackend,
    val mono: MultipleMonoSettings,
    val bot: TelegramBotSettings,
    val mcc: MccOverride,
) {
    companion object {
        fun load(path: Path): Settings = load(File(path.toString()).readText())

        internal fun load(content: String) = Json.decodeFromString<Settings>(content).also {
            logger.debug { "Loaded settings: $it" }
        }
    }

    @Serializable
    data class MultipleMonoSettings(
        val settings: List<MonoSettings>,
    ) {
        val apis: List<MonoApi> by lazy {
            settings.map { it.token }.map(::MonoApi)
        }
        val byId: Map<String, MonoSettings> by lazy {
            settings.associateBy { it.accountId }
        }
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
        val mccToCategoryName: Map<Int, String>
    )
}
