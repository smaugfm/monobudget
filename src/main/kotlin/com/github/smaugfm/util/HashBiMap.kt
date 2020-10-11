package com.github.smaugfm.util

import io.michaelrocks.bimap.AbstractBiMap
import java.util.*

class HashBiMap<K : Any, V : Any>(capacity: Int = 16) : AbstractBiMap<K, V>(HashMap(capacity), HashMap(capacity)) {
    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is HashBiMap<*, *>) return false

        return this.toMap() == other.toMap()
    }

    override fun hashCode(): Int {
        return this.toMap().hashCode()
    }

    companion object {
        fun <K : Any, V : Any> of(map: Map<K, V>): HashBiMap<K, V> {
            val bimap = HashBiMap<K, V>()
            bimap.putAll(map)
            return bimap
        }

        fun <K : Any, V : Any> of(vararg pairs: Pair<K, V>) = this.of(pairs.toMap())
    }
}
