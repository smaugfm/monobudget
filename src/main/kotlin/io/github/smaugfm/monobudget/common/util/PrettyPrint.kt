package io.github.smaugfm.monobudget.common.util

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.lang.reflect.Type
import java.util.Currency

fun <T : Any> T.pp(): String = this::class.simpleName + "@" + gson.toJson(this)!!

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
                ): JsonElement = context.serialize(src.currencyCode)
            },
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
            },
        )
        .registerTypeAdapter(
            LocalDate::class.java,
            object : JsonSerializer<LocalDate> {
                override fun serialize(
                    src: LocalDate,
                    typeOfSrc: Type,
                    context: JsonSerializationContext,
                ): JsonElement = context.serialize(src.toString())
            },
        )
        .create()
