package com.github.smaugfm.models.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class HumanReadableDateSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

    override val descriptor = PrimitiveSerialDescriptor(
        this::class.qualifiedName!!,
        PrimitiveKind.LONG
    )

    override fun deserialize(decoder: Decoder): LocalDateTime =
        LocalDateTime.parse(
            decoder.decodeString(),
            formatter
        )

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(formatter.format(value))
    }
}
