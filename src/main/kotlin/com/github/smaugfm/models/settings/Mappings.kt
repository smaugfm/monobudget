@file:UseSerializers(CurrencyAsStringSerializer::class)

package com.github.smaugfm.models.settings

import com.github.smaugfm.models.MonoAccountId
import com.github.smaugfm.models.serializers.CurrencyAsStringSerializer
import com.github.smaugfm.models.serializers.HashBiMapAsMapSerializer
import com.uchuhimo.collections.BiMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import mu.KotlinLogging
import java.util.Currency

private val logger = KotlinLogging.logger { }

@Serializable
data class Mappings(
    private val monoAccAliases: Map<String, String>,
    @Serializable(with = HashBiMapAsMapSerializer::class)
    private val monoAccToYnab: BiMap<String, String>,
    private val monoAccToCurrency: Map<String, Currency>,
    @Serializable(with = HashBiMapAsMapSerializer::class)
    private val monoAccToTelegram: BiMap<String, Long>,
    private val mccToCategory: Map<Int, String>,
    val unknownPayeeId: String,
    val unknownCategoryId: String,
) {
    fun getMonoAccounts() = monoAccToTelegram.keys
    fun getTelegramChatIds() = monoAccToTelegram.values.toSet()

    fun getMccCategoryOverride(mccCode: Int): String? = mccToCategory[mccCode]

    fun getAccountCurrency(monoAccountId: MonoAccountId): Currency? =
        monoAccToCurrency[monoAccountId]

    fun getYnabAccByMono(monoAcc: String): String? =
        monoAccToYnab[monoAcc].also {
            if (it == null)
                logger.error { "Could not find YNAB account for Monobank account $monoAcc" }
        }

    fun getTelegramChatIdAccByMono(monoAcc: String) =
        monoAccToTelegram[monoAcc].also {
            if (it == null)
                logger.error { "Could not find Telegram chatID for Monobank account $monoAcc" }
        }

    fun getMonoAccAlias(string: MonoAccountId): String? {
        return monoAccAliases[string].also {
            if (it == null)
                logger.error { "Could not find alias for Monobank account $string" }
        }
    }
}
