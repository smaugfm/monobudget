package com.github.smaugfm.mono

import com.github.smaugfm.util.MCC
import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import com.github.smaugfm.serializers.InstantAsLongSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.*

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
) {

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("MonoStatementItem {\n")
        builder.append("\tid: $id\n")
        builder.append("\tdesc: $description\n")
        builder.append("\tamount: $amount\n")
        builder.append("\tmcc:$mcc (${MCC.mapRussian.getOrDefault(mcc, "unknown")})\n")
        builder.append("\ttime: $time\n")
        if (comment.isNotBlank())
            builder.append("\tcomment: $comment\n")
        builder.append("\tbalance: $balance\n")
        builder.append("\toperationAmount: $operationAmount\n")
        builder.append("\tcommisionRate: $commissionRate\n")
        builder.append("\tcashbackAmount: $cashbackAmount\n")
        builder.append("\tcurrency: $currencyCode\n")
        builder.append("\thold: $hold\n}\n")

        return builder.toString()
    }
}

