package com.github.smaugfm.util

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.milliseconds
import kotlin.time.toJavaDuration

class ExpirtyContainerTest {

    @Test
    fun simple() {
        val delay = 100L
        val tolerance = 10

        val container = ExpiryContainer<Int>(delay.milliseconds.toJavaDuration())

        fun checkOne(item: Int) {
            fun contains(): Boolean =
                container.consumeCollection {
                    contains(item)
                }

            container.add(item)
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
