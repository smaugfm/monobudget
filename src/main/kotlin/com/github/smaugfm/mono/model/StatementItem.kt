package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

/**
 * Перелік транзакцій за вказанний час
 */

@Serializable
data class StatementItem(
    val id: String,
    val time: Long,
    val description: String,
    val mcc: Int,
    val amount: Long,
    val operationAmount: Long,
    val currencyCode: Int,
    val commissionRate: Long,
    val cashbackAmount: Long,
    val balance: Long,
    val hold: Boolean
)