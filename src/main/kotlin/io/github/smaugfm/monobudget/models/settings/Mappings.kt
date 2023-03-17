@file:UseSerializers(CurrencyAsStringSerializer::class)

package io.github.smaugfm.monobudget.models.settings

import com.uchuhimo.collections.BiMap
import io.github.smaugfm.monobudget.models.serializer.CurrencyAsStringSerializer
import io.github.smaugfm.monobudget.models.serializer.HashBiMapAsMapSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.util.Currency

@Serializable
data class Mappings(
    val monoAccAliases: Map<String, String>,
    @Serializable(with = HashBiMapAsMapSerializer::class)
    val monoAccToYnab: BiMap<String, String>,
    val monoAccToCurrency: Map<String, Currency>,
    @Serializable(with = HashBiMapAsMapSerializer::class)
    val monoAccToTelegram: BiMap<String, Long>,
    val mccToCategory: Map<Int, String>,
    val unknownPayeeId: String,
    val unknownCategoryId: String
)
