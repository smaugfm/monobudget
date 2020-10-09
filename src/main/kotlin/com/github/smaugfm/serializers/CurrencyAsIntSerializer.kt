package com.github.smaugfm.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

class CurrencyAsIntSerializer : KSerializer<Currency> {
    override val descriptor = PrimitiveSerialDescriptor(
        this::class.qualifiedName!!,
        PrimitiveKind.INT
    )

    override fun deserialize(decoder: Decoder): Currency {
        val numericCode = decoder.decodeInt()
        return Currency.getAvailableCurrencies().find { it.numericCode == numericCode }!!
    }

    override fun serialize(encoder: Encoder, value: Currency) {
        return encoder.encodeInt(value.numericCode)
    }

}