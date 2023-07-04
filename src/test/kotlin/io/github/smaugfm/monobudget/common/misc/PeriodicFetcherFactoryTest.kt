package io.github.smaugfm.monobudget.common.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import kotlin.time.Duration.Companion.milliseconds

class PeriodicFetcherFactoryTest {

    private val factory = PeriodicFetcherFactory(GlobalScope)

    private val values = generateSequence(0) { it + 1 }.iterator()

    @RepeatedTest(100)
    fun test() {
        val def = CompletableDeferred(Unit)
        val fetcher = factory.PeriodicFetcher("TestFetcher", 10.milliseconds) {
            def.await()
            values.next()
        }

        runBlocking {
            def.complete(Unit)
            assertThat(fetcher.getData()).isEqualTo(0)
            delay(15.milliseconds)
            assertThat(fetcher.getData()).isGreaterThan(0)
        }
    }
}
