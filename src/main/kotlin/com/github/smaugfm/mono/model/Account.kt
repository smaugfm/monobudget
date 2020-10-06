package com.github.smaugfm.mono.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: AccountId,
    val balance: Long,
    val creditLimit: Long,
    val currencyCode: Int,
    val cashbackType: String,
    val iban: String,
    val maskedPan: List<String>,
    val type: String
)