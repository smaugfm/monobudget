package com.github.smaugfm.settings

import com.github.smaugfm.serializers.HashBiMapAsMapSerializer
import io.michaelrocks.bimap.BiMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.logging.Logger

@Serializable
data class Mappings(
    @Serializable(with = HashBiMapAsMapSerializer::class)
    private val monoAcc2Ynab: BiMap<String, String>,
    private val monoAcc2Telegram: Map<String, Long>,
    private val mccToCategory: Map<Int, String>,
) {
    @Transient
    private val logger = Logger.getLogger(Mappings::class.simpleName)

    fun getMonoAccounts(): Set<String> = monoAcc2Telegram.keys

    fun getYnabAccByMono(monoAcc: String): String? = monoAcc2Ynab[monoAcc].also {
        if (it == null)
            logger.severe("Could not find YNAB account for Mono account $monoAcc")
    }

    fun getTelegramChaIdAccByMono(monoAcc: String): Long? = monoAcc2Telegram[monoAcc].also {
        if (it == null)
            logger.severe("Could not find Telegram chatID for Mono account $monoAcc")
    }
}