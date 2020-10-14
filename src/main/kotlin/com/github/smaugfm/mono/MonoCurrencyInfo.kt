package com.github.smaugfm.mono

import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import com.github.smaugfm.serializers.InstantAsLongSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Currency

/**
 * Перелік курсів. Кожна валютна пара може мати одне і більше полів з rateSell, rateBuy, rateCross.
 */

@Serializable
data class MonoCurrencyInfo(
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCodeA: Currency,
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCodeB: Currency,
    @Serializable(InstantAsLongSerializer::class)
    val date: Instant,
    val rateSell: Double? = null,
    val rateBuy: Double? = null,
    val rateCross: Double? = null,
)
