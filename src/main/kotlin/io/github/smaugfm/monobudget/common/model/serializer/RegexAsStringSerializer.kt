package io.github.smaugfm.monobudget.common.model.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class RegexAsStringSerializer : KSerializer<Regex> {
    override val descriptor = PrimitiveSerialDescriptor(this::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Regex {
        val regexStr = decoder.decodeString()
        return Regex(regexStr)
    }

    override fun serialize(encoder: Encoder, value: Regex) {
        encoder.encodeString(value.pattern)
    }
}
