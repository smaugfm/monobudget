package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

/**
 * Перелік курсів. Кожна валютна пара може мати одне і більше полів з rateSell, rateBuy, rateCross.
 */

@Serializable
data class CurrencyInfo(
    val currencyCodeA: Int,
    val currencyCodeB: Int,
    val date: Long,
    val rateSell: Double? = null,
    val rateBuy: Double? = null,
    val rateCross: Double? = null
)