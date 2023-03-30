package io.github.smaugfm.monobudget.common.misc

import io.ktor.util.logging.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

private val log = KotlinLogging.logger {}

@Single
class PeriodicFetcherFactory : KoinComponent {
    private val scope: CoroutineScope by inject()

    fun <T> create(name: String, fetch: suspend () -> T) = PeriodicFetcher(name, fetch, 1.hours)

    inner class PeriodicFetcher<T>(
        name: String,
        fetch: suspend () -> T,
        interval: Duration
    ) {
        private val initial = CompletableDeferred<T>()

        @Volatile
        private var data: Deferred<T> = initial

        suspend fun getData() = data.await()

        init {
            log.info { "Launching periodic fetcher $name" }
            data = initial
            scope.launch(context = Dispatchers.IO) {
                while (true) {
                    log.debug { "$name fetching..." }
                    val result = try {
                        fetch()
                    } catch (e: Throwable) {
                        log.error(e)
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
