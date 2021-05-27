package com.github.smaugfm.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class ExpiringMap<T, K>(val expirationTime: Duration) {
    private val map = mutableMapOf<T, K>()
    private val coroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    @Synchronized
    fun add(item: T, value: K) {
        map[item] = value
        coroutineScope.launch {
            delay(expirationTime.toJavaDuration())
            expire(item)
        }
    }

    @Synchronized
    private fun expire(item: T) {
        map.remove(item)
    }

    @Synchronized
    fun <R> consumeCollection(block: Map<T, K>.() -> R): R = map.block()
}
