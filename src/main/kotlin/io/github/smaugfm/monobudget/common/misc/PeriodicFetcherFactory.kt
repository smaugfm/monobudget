package io.github.smaugfm.monobudget.common.misc

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val log = KotlinLogging.logger {}

@Single
class PeriodicFetcherFactory(private val scope: CoroutineScope) {
    fun <T> create(name: String, fetch: suspend () -> T) = PeriodicFetcher(name, 1.hours, fetch)

    inner class PeriodicFetcher<T>(
        name: String,
        interval: Duration,
        fetch: suspend () -> T
    ) {
        private val initial = CompletableDeferred<T>()

        @Volatile
        private var data: Deferred<T> = initial

        suspend fun getData() = data.await()

        init {
            log.info { "Launching periodic fetcher for $name" }
            scope.launch {
                while (true) {
                    log.debug { "$name fetching..." }
                    val result = try {
                        fetch()
                    } catch (e: Throwable) {
                        log.error(e) { "Error fetching $name: " }
                        continue
                    }
                    if (data === initial) {
                        initial.complete(result)
                    }
                    data = CompletableDeferred(result)
                    log.debug { "$name fetched: $result" }
                    delay(interval)
                }
            }
        }
    }
}
