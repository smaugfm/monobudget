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
    private val monoAccToYnab: BiMap<String, String>,
    private val monoAccToCurrency: Map<String, Currency>,
    private val monoAccToTelegram: Map<String, Int>,
    private val mccToCategory: Map<Int, String>,
    val unknownPayeeId: String,
    val unknownCategoryId: String,
) {
    fun getMonoAccounts(): Set<String> = monoAccToTelegram.keys
    fun getTelegramChatIds(): Set<Int> = monoAccToTelegram.values.toSet()

    fun getMccCategoryOverride(mccCode: Int): String? = mccToCategory[mccCode]

    fun getAccountCurrency(monoAccountId: MonoAccountId): Currency? =
        monoAccToCurrency[monoAccountId]

    fun getYnabAccByMono(monoAcc: String): String? =
        monoAccToYnab[monoAcc].also {
            if (it == null)
                logger.error("Could not find YNAB account for Mono account $monoAcc")
        }

    fun getTelegramChatIdAccByMono(monoAcc: String): Int? =
        monoAccToTelegram[monoAcc].also {
            if (it == null)
                logger.error("Could not find Telegram chatID for Mono account $monoAcc")
        }

    companion object {
        val Empty =
            Mappings(emptyBiMap(), emptyMap(), emptyMap(), emptyMap(), "", "")
    }
}
