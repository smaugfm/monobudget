package com.github.smaugfm.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI

class URIAsStringSerializer : KSerializer<URI> {
    override val descriptor =
        PrimitiveSerialDescriptor(URIAsStringSerializer::class.qualifiedName.toString(), PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = URI(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }
}