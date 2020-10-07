package com.github.smaugfm.mono.model

import com.github.smaugfm.mono.model.serializers.CurrencyAsIntSerializer
import com.github.smaugfm.mono.model.serializers.InstantAsLongSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.*

/**
 * Перелік курсів. Кожна валютна пара може мати одне і більше полів з rateSell, rateBuy, rateCross.
 */

@Serializable
data class CurrencyInfo(
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCodeA: Currency,
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCodeB: Currency,
    @Serializable(InstantAsLongSerializer::class)
    val date: Instant,
    val rateSell: Double? = null,
    val rateBuy: Double? = null,
    val rateCross: Double? = null
)