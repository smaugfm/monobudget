package com.github.smaugfm.mono.model

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Перелік транзакцій за вказанний час
 */

@Serializable
data class StatementItem(
    val id: String,
    @Serializable(with = InstantAsLongSerializer::class)
    val time: Instant,
    val description: String,
    val mcc: Int,
    val amount: Long,
    val operationAmount: Long,
    val currencyCode: Int,
    val commissionRate: Long,
    val cashbackAmount: Long,
    val balance: Long,
    val hold: Boolean
)

class InstantAsLongSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("time", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder) = Instant.fromEpochSeconds(decoder.decodeLong())

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochSeconds)
    }

}
