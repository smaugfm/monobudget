package io.github.smaugfm.monobudget.integration

import io.github.smaugfm.lunchmoney.exception.LunchmoneyApiResponseException
import io.ktor.http.HttpStatusCode
import reactor.core.publisher.Mono
import java.util.function.Function

class FailTrackerTransformation<T>(private val configs: List<IntegrationFailConfig>) :
    Function<Mono<T>, Mono<T>> {
    private var attempt = 0

    override fun apply(mono: Mono<T>): Mono<T> =
        (
            if (configs.any { it.attemptFailRange.contains(attempt) }) {
                Mono.error(LunchmoneyApiResponseException(HttpStatusCode.BadRequest.value))
            } else {
                mono
            }
        )
            .also { attempt++ }
}
