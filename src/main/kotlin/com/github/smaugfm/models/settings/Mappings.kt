@file:UseSerializers(CurrencyAsStringSerializer::class)

package com.github.smaugfm.models.settings

import com.github.smaugfm.models.serializer.CurrencyAsStringSerializer
import com.github.smaugfm.models.serializer.HashBiMapAsMapSerializer
import com.uchuhimo.collections.BiMap
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
    val unknownCategoryId: String,
)
