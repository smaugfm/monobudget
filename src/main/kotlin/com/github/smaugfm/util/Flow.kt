package com.github.smaugfm.util

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

@OptIn(InternalCoroutinesApi::class)
fun <T> Flow<T>.throttle(periodMillis: Long): Flow<T> {
    require(periodMillis > 0) { "period should be positive" }
    return flow {
        var lastTime = 0L
        collect {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime >= periodMillis) {
                lastTime = currentTime
                emit(it)
            }
        }
    }
}

fun <T> Flow<T>.chunked(size: Int): Flow<List<T>> = flow {
    val chunkedList = mutableListOf<T>()
    collect {
        chunkedList.add(it)
        if (chunkedList.size == size) {
            emit(chunkedList.toList())
            chunkedList.clear()
        }
    }
}
