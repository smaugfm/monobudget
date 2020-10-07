package com.github.smaugfm.mono.model

import com.github.smaugfm.mono.model.serializers.CurrencyAsIntSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Account(
    val id: AccountId,
    val balance: Long,
    val creditLimit: Long,
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCode: Currency,
    val cashbackType: CashbackType,
    val iban: String,
    val maskedPan: List<String>,
    val type: String
)

