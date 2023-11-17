package io.github.smaugfm.monobudget.common.misc

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ExpiryContainerTest {
    @Test
    fun expiryContainerSimpleTest() {
        val delay = 100L
        val tolerance = 10

        val container = ConcurrentExpiringMap<String, Int>(delay.milliseconds)

        fun checkOne(item: Int) {
            fun contains(): Boolean = container.contains(item.toString())

            container.add(item.toString(), item)
            assert(contains())
            Thread.sleep(delay + tolerance)
            assert(!contains())
        }

        runBlocking {
            listOf(1, 2, 3).forEach { item ->
                launch {
                    checkOne(item)
                }
                delay(delay)
            }
        }
    }
}
