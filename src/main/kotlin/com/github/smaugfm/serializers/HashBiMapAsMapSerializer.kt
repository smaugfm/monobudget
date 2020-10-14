package com.github.smaugfm.serializers

import com.github.smaugfm.util.HashBiMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class HashBiMapAsMapSerializer<K : Any, V : Any>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
) : KSerializer<HashBiMap<K, V>> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)
    override val descriptor = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): HashBiMap<K, V> =
        HashBiMap.of(decoder.decodeSerializableValue(mapSerializer))

    override fun serialize(encoder: Encoder, value: HashBiMap<K, V>) {
        encoder.encodeSerializableValue(mapSerializer, value)
    }
}
