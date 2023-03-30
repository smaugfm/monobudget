package io.github.smaugfm.monobudget.common.misc

import java.util.concurrent.ConcurrentHashMap

class SimpleCache<K, V>(private val getValue: suspend (K) -> V) {
    private val cache = ConcurrentHashMap<K, V>()

    suspend fun get(key: K): V = cache.getOrPut(key) {
        getValue(key)
    }

    fun alreadyHasKey(key: K, v: V): Boolean = cache.putIfAbsent(key, v) == null
}
