package com.github.smaugfm.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.ktor.client.plugins.ResponseException
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.json.JsonNamingStrategy
import mu.KLogger
import java.lang.reflect.Type
import java.util.Currency
import kotlin.math.abs
import kotlin.math.pow

fun Number.formatW(): String {
    return "%02d".format(this)
}

const val DEFAULT_HTTP_PORT = 80

fun Currency.formatAmount(amount: Long): String {
    val delimiter = (10.0.pow(defaultFractionDigits)).toInt()
    return "${amount / delimiter}.${(abs(amount % delimiter).formatW())}"
}

fun String.replaceNewLines(): String =
    replace("\n", " ").replace("\r", "")

fun makeJson(convertSnakeCase: Boolean = false): Json =
    Json { buildJson(convertSnakeCase) }

@OptIn(ExperimentalSerializationApi::class)
fun JsonBuilder.buildJson(convertSnakeCase: Boolean = false) {
    if (convertSnakeCase)
        namingStrategy = JsonNamingStrategy.SnakeCase
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Suppress("LongParameterList")
suspend inline fun <reified T : Any> logError(
    serviceName: String,
    logger: KLogger?,
    methodName: String,
    json: Json,
    block: () -> T,
    error: (ResponseException) -> Unit,
) =
    catchAndLog(
        logger,
        methodName,
        { exception ->
            exception
                .also(error)
                .toString()
        }
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

private val gson =
    GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(
            Currency::class.java,
            object : JsonSerializer<Currency> {
                override fun serialize(
                    src: Currency,
                    typeOfSrc: Type,
                    context: JsonSerializationContext,
                ): JsonElement =
                    context.serialize(src.currencyCode)
            }
        )
        .registerTypeAdapter(
            Instant::class.java,
            object : JsonSerializer<Instant> {
                override fun serialize(
                    src: Instant,
                    typeOfSrc: Type,
                    context: JsonSerializationContext,
                ): JsonElement =
                    context.serialize(src.toLocalDateTime(TimeZone.currentSystemDefault()).toString())
            }
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            object : JsonSerializer<LocalDate> {
                override fun serialize(
                    src: LocalDate,
                    typeOfSrc: Type,
                    context: JsonSerializationContext,
                ): JsonElement =
                    context.serialize(src.toString())
            }
        )
        .create()

fun <T : Any> T.pp(): String = this::class.simpleName + "@" + gson.toJson(this)!!
