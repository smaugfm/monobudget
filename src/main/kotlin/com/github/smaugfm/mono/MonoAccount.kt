package com.github.smaugfm.mono

import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import kotlinx.serialization.Serializable
import java.util.*

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
) {
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("MonoAccount {\n")
        builder.append("\tid: $id\n")
        builder.append("\tbalance: $balance\n")
        builder.append("\tcreditLimit: $creditLimit\n")
        builder.append("\tcurrencyCode: $currencyCode\n")
        builder.append("\tcashbackType: $cashbackType\n")
        builder.append("\tiban: $iban\n")
        builder.append("\tmaskedPan: $maskedPan\n")
        builder.append("\ttype: $type\n")
        builder.append("}")

        return builder.toString()
    }
}

