package io.github.smaugfm.monobudget.common.model.serializer

import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class LocalDateAsISOSerializer : KSerializer<LocalDate> {
    override val descriptor =
        PrimitiveSerialDescriptor(
            LocalDateAsISOSerializer::class.qualifiedName.toString(),
            PrimitiveKind.STRING,
        )

    override fun serialize(
        encoder: Encoder,
        value: LocalDate,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString())
    }
}
