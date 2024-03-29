package io.github.smaugfm.monobudget.integration.util

import io.github.smaugfm.lunchmoney.exception.LunchmoneyApiResponseException
import io.ktor.http.HttpStatusCode
import reactor.core.publisher.Mono
import java.util.function.Function

class FailTrackerTransformation<T>(private val configs: List<IntegrationFailConfig>) :
    Function<Mono<T>, Mono<T>> {
    private var attempt = 0

    override fun apply(mono: Mono<T>): Mono<T> =
        mono.takeIf { configs.all { !it.attemptFailRange.contains(attempt++) } }
            ?: Mono.error(LunchmoneyApiResponseException(HttpStatusCode.BadRequest.description))
}
