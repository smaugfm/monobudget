package com.github.smaugfm.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.Duration

class ExpiryContainerTest {

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun simple() {
        val delay = 100L
        val tolerance = 10

        val container = ExpiringMap<String, Int>(Duration.milliseconds(delay))

        fun checkOne(item: Int) {
            fun contains(): Boolean =
                container.consumeCollection {
                    contains(item.toString())
                }

            container.add(item.toString(), item)
            assert(contains())
            Thread.sleep(delay + tolerance)
            assert(!contains())
        }

        runBlocking {
            listOf(1, 2, 3).forEach { item ->
                GlobalScope.launch {
                    checkOne(item)
                }
                delay(delay)
            }
        }
    }
}
