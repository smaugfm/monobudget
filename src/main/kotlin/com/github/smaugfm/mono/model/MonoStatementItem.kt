package com.github.smaugfm.mono.model

import com.github.smaugfm.serializers.InstantAsLongSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
    val currencyCode: Int,
    val commissionRate: Long,
    val cashbackAmount: Long,
    val balance: Long,
    val hold: Boolean,
)

