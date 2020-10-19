package com.github.smaugfm.settings

import com.github.smaugfm.serializers.HashBiMapAsMapSerializer
import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.logging.Logger

@Serializable
data class Mappings(
    @Serializable(with = HashBiMapAsMapSerializer::class)
    private val monoAcc2Ynab: BiMap<String, String>,
    private val monoAcc2Telegram: Map<String, Int>,
    private val mccToCategory: Map<Int, String>,
    private val descToPayee: Map<String, String>,
    val unknownPayeeId: String,
    val unknownCategoryId: String,
) {
    @Transient
    private val logger = Logger.getLogger(Mappings::class.simpleName)

    fun getMonoAccounts(): Set<String> = monoAcc2Telegram.keys
    fun getTelegramChatIds(): Set<Int> = monoAcc2Telegram.values.toSet()

    fun getYnabAccByMono(monoAcc: String): String? = monoAcc2Ynab[monoAcc].also {
        if (it == null)
            logger.severe("Could not find YNAB account for Mono account $monoAcc")
    }

    fun getTelegramChatIdAccByMono(monoAcc: String): Int? = monoAcc2Telegram[monoAcc].also {
        if (it == null)
            logger.severe("Could not find Telegram chatID for Mono account $monoAcc")
    }

    companion object {
        val Empty = Mappings(HashBiMap(), emptyMap(), emptyMap(), emptyMap(), "", "")
    }
}
