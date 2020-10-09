package com.github.smaugfm.serializers

import io.michaelrocks.bimap.BiMap
import io.michaelrocks.bimap.HashBiMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class HashBiMapAsMapSerializer<K : Any, V : Any>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : KSerializer<HashBiMap<K, V>> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)
    override val descriptor = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): HashBiMap<K, V> {
        val map = decoder.decodeSerializableValue(mapSerializer)
        return HashBiMap.create(map)
    }

    override fun serialize(encoder: Encoder, value: HashBiMap<K, V>) {
        encoder.encodeSerializableValue(mapSerializer, value)
    }

}