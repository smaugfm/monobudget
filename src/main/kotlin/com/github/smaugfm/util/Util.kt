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
import kotlin.math.max
import kotlin.math.min
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
inline fun <reified T : Any> logError(
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

@Suppress("ComplexMethod", "MagicNumber", "LoopWithTooManyJumpStatements", "ReturnCount")
fun jaroSimilarity(s1: String, s2: String): Double {
    if (s1.isEmpty() && s2.isEmpty()) return 1.0
    else if (s1.isEmpty() || s2.isEmpty()) return 0.0
    else if (s1.length == 1 && s2.length == 1) return if (s1[0] == s2[0]) 1.0 else 0.0

    val searchRange: Int = max(s1.length, s2.length) / 2 - 1
    val s2Consumed = BooleanArray(s2.length)
    var matches = 0.0
    var transpositions = 0
    var s2MatchIndex = 0

    for ((i, c1) in s1.withIndex()) {
        val start = max(0, i - searchRange)
        val end = min(s2.lastIndex, i + searchRange)
        for (j in start..end) {
            val c2 = s2[j]
            if (c1 != c2 || s2Consumed[j]) continue
            s2Consumed[j] = true
            matches += 1
            if (j < s2MatchIndex) transpositions += 1
            s2MatchIndex = j
            break
        }
    }

    return when (matches) {
        0.0 -> 0.0
        else -> (
            matches / s1.length +
                matches / s2.length +
                (matches - transpositions) / matches
            ) / 3.0
    }
}

fun jaroWinklerSimilarity(s1: String, s2: String, ignoreCase: Boolean): Double {
    // Unlike classic Jaro-Winkler, we don't set a limit on the prefix length
    val prefixLength = s1.commonPrefixWith(s2, ignoreCase).length
    val case = { s: String -> if (ignoreCase) s.lowercase() else s }
    val jaro = jaroSimilarity(case(s1), case(s2))
    val winkler = jaro + (0.1 * prefixLength * (1 - jaro))
    return min(winkler, 1.0)
}
