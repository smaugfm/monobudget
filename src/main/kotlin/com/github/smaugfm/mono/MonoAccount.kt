package com.github.smaugfm.mono

import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class MonoAccount(
    val id: MonoAccountId,
    val balance: Long,
    val creditLimit: Long,
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCode: Currency,
    val cashbackType: MonoCashbackType,
    val iban: String,
    val maskedPan: List<String>,
    val type: String,
)
