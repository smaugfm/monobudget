package com.github.smaugfm.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.concurrent.Executors

class ExpiryContainer<T>(val expiry: Duration) {
    private val map = mutableMapOf<Long, T>()
    private var counter = 0L
    private val coroutineScope =
        CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    @Synchronized
    fun add(item: T) {
        val actualCounter = counter++
        map[actualCounter] = item
        coroutineScope.launch {
            delay(expiry)
            expire(actualCounter)
        }
    }

    @Synchronized
    private fun expire(counter: Long) {
        map.remove(counter)
    }

    @Synchronized
    fun <K> consumeCollection(block: Collection<T>.() -> K) = map.values.block()
}
