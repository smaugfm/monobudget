package io.github.smaugfm.monobudget.model.serializer

import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class HashBiMapAsMapSerializer<K : Any, V : Any>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>
) : KSerializer<BiMap<K, V>> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)
    override val descriptor = mapSerializer.descriptor

    override fun deserialize(decoder: Decoder): BiMap<K, V> = decoder.decodeSerializableValue(mapSerializer).toBiMap()

    override fun serialize(encoder: Encoder, value: BiMap<K, V>) {
        encoder.encodeSerializableValue(mapSerializer, value)
    }
}
