@file:UseSerializers(CurrencyAsStringSerializer::class)

package com.github.smaugfm.settings

import com.github.smaugfm.mono.MonoAccountId
import com.github.smaugfm.serializers.CurrencyAsStringSerializer
import com.github.smaugfm.serializers.HashBiMapAsMapSerializer
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.emptyBiMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import mu.KotlinLogging
import java.util.Currency

private val logger = KotlinLogging.logger { }

@Serializable
data class Mappings(
    @Serializable(with = HashBiMapAsMapSerializer::class)
    private val monoAcc2Ynab: BiMap<String, String>,
    private val monoAccToCurrency: Map<String, Currency>,
    private val monoAcc2Telegram: Map<String, Int>,
    private val mccToCategory: Map<Int, String>,
    val unknownPayeeId: String,
    val unknownCategoryId: String,
) {
    fun getMonoAccounts(): Set<String> = monoAcc2Telegram.keys
    fun getTelegramChatIds(): Set<Int> = monoAcc2Telegram.values.toSet()

    fun getMccCategoryOverride(mccCode: Int): String? = mccToCategory[mccCode]

    fun getAccountCurrency(monoAccountId: MonoAccountId): Currency? =
        monoAccToCurrency[monoAccountId]

    fun getYnabAccByMono(monoAcc: String): String? =
        monoAcc2Ynab[monoAcc].also {
            if (it == null)
                logger.error("Could not find YNAB account for Mono account $monoAcc")
        }

    fun getTelegramChatIdAccByMono(monoAcc: String): Int? =
        monoAcc2Telegram[monoAcc].also {
            if (it == null)
                logger.error("Could not find Telegram chatID for Mono account $monoAcc")
        }

    companion object {
        val Empty =
            Mappings(emptyBiMap(), emptyMap(), emptyMap(), emptyMap(), "", "")
    }
}
