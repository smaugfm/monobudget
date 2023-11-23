package io.github.smaugfm.monobudget.common.util

import io.github.oshai.kotlinlogging.KLogger
import io.ktor.client.plugins.ResponseException

@Suppress("LongParameterList")
inline fun <reified T : Any> logError(
    serviceName: String,
    logger: KLogger?,
    methodName: String,
    block: () -> T,
    error: (ResponseException) -> Unit,
) = catchAndLog(
    logger,
    methodName,
    { exception ->
        exception
            .also(error)
            .toString()
    },
) {
    logger?.debug { "Performing $serviceName request $methodName" }
    block().also {
        logger?.debug { "Response:\n\t${it.pp()}" }
    }
}

inline fun <reified T> catchAndLog(
    logger: KLogger?,
    methodName: String,
    errorHandler: (ResponseException) -> String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: ResponseException) {
        val error = errorHandler(e)
        logger?.error { "Request failed $methodName. Error response:\n\t$error" }
        throw e
    }
