package com.github.smaugfm.util

import com.github.smaugfm.ynab.YnabErrorResponse
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import io.ktor.client.features.ResponseException
import io.ktor.client.statement.readText
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KLogger
import java.lang.reflect.Type
import java.util.Currency
import kotlin.math.abs
import kotlin.math.pow

fun Number.formatW(): String {
    return "%02d".format(this)
}

fun Currency.formatAmount(amount: Long): String {
    val delimiter = (10.0.pow(defaultFractionDigits)).toInt()
    return "${amount / delimiter}.${(abs(amount % delimiter).formatW())}"
}

fun String.replaceNewLines(): String =
    replace("\n", " ").replace("\r", "")

fun makeJson(): Json =
    Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

suspend inline fun <reified T : Any> requestCatching(
    serviceName: String,
    logger: KLogger,
    methodName: String,
    json: Json,
    block: () -> T,
) =
    catchAndLog(
        logger,
        methodName,
        { exception ->
            StringBuilder().also {
                json.decodeFromString<YnabErrorResponse>(exception.response.readText()).error.pp()
            }.toString()
        }
    ) {
        logger.info("Performing $serviceName request $methodName")
        block().also {
            logger.info("Response:\n\t${it.pp()}")
        }
    }

inline fun <reified T> catchAndLog(
    logger: KLogger,
    methodName: String,
    errorHandler: (ResponseException) -> String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: ResponseException) {
        val error = errorHandler(e)
        logger.info("Request failed $methodName. Error response:\n\t$error")
        throw e
    }

suspend inline fun <reified T> catchAndLog(
    logger: KLogger,
    methodName: String,
    block: () -> T,
): T =
    try {
        block()
    } catch (e: ResponseException) {
        val error = e.response.readText()
        logger.info("Request failed $methodName. Error response:\n\t$error")
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
