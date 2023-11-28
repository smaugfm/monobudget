package io.github.smaugfm.monobudget.common.util.misc

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

@Single
class PeriodicFetcherFactory(private val scope: CoroutineScope) {
    fun <T> create(
        name: String,
        fetch: suspend () -> T,
    ) = PeriodicFetcher(name, 1.hours, scope, fetch)

    class PeriodicFetcher<T>(
        name: String,
        interval: Duration,
        scope: CoroutineScope,
        fetch: suspend () -> T,
    ) {
        private val initial = CompletableDeferred<T>()

        @Volatile
        private var fetched: Deferred<T> = initial

        suspend fun fetched() =
            withTimeout(5.seconds) {
                fetched.await()
            }

        init {
            log.info { "Launching periodic fetcher for $name" }
            scope.launch {
                while (true) {
                    log.trace { "$name fetching..." }
                    val result =
                        try {
                            fetch()
                        } catch (e: Throwable) {
                            log.error(e) { "Error fetching $name: " }
                            delay(interval)
                            continue
                        }
                    if (fetched === initial) {
                        initial.complete(result)
                    }
                    fetched = CompletableDeferred(result)
                    log.trace { "$name fetched: $result" }
                    delay(interval)
                }
            }
        }
    }
}
