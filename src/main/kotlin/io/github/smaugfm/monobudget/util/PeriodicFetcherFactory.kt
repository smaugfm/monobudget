package io.github.smaugfm.monobudget.util

import io.ktor.util.logging.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val logger = KotlinLogging.logger {}

class PeriodicFetcherFactory(
    private val scope: CoroutineScope,
) {
    fun <T> create(
        name: String,
        fetch: suspend () -> T
    ) = PeriodicFetcher(name, fetch, 1.hours)

    inner class PeriodicFetcher<T>(
        name: String,
        fetch: suspend () -> T,
        interval: Duration,
    ) {
        private val initial = CompletableDeferred<T>()

        @Volatile
        var data: Deferred<T> = initial
            private set

        init {
            logger.info { "Launching periodic fetcher $name" }
            data = initial
            scope.launch(context = Dispatchers.IO) {
                while (true) {
                    logger.debug { "$name fetching..." }
                    val result = try {
                        fetch()
                    } catch (e: Throwable) {
                        logger.error(e)
                        continue
                    }
                    if (data === initial) {
                        initial.complete(result)
                    } else {
                        data = CompletableDeferred(result)
                    }
                    delay(interval)
                }
            }
        }
    }
}
