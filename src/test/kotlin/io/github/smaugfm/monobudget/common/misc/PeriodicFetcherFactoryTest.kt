package io.github.smaugfm.monobudget.common.misc

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.github.smaugfm.monobudget.common.util.misc.PeriodicFetcherFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.RepeatedTest
import kotlin.time.Duration.Companion.milliseconds

@OptIn(DelicateCoroutinesApi::class)
class PeriodicFetcherFactoryTest {
    private val values = generateSequence(0) { it + 1 }.iterator()

    @RepeatedTest(100)
    fun test() {
        val def = CompletableDeferred(Unit)

        val fetcher =
            PeriodicFetcherFactory.PeriodicFetcher("TestFetcher", 10.milliseconds, GlobalScope) {
                def.await()
                values.next()
            }

        runBlocking {
            def.complete(Unit)
            assertThat(fetcher.fetched()).isEqualTo(0)
            delay(30.milliseconds)
            assertThat(fetcher.fetched()).isGreaterThan(0)
        }
    }
}
