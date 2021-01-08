package com.github.smaugfm.mono

import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import com.github.smaugfm.serializers.InstantAsLongSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.Currency

/**
 * Перелік транзакцій за вказанний час
 */

@Serializable
data class MonoStatementItem(
    val id: String,
    @Serializable(with = InstantAsLongSerializer::class)
    val time: Instant,
    val description: String,
    val mcc: Int,
    val amount: Long,
    val operationAmount: Long,
    @Serializable(with = CurrencyAsIntSerializer::class)
    val currencyCode: Currency,
    val comment: String = "",
    val commissionRate: Long,
    val cashbackAmount: Long,
    val balance: Long,
    val hold: Boolean,
)
