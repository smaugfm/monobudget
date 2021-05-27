package com.github.smaugfm.mono

import com.github.smaugfm.serializers.CurrencyAsIntSerializer
import com.github.smaugfm.serializers.InstantAsLongSerializer
import com.github.smaugfm.util.IErrorFormattable
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Currency

typealias MonoAccountId = String

@Serializable
enum class MonoCashbackType {
    @SerialName("")
    None,
    UAH,
    Miles
}

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

/**
 * Текст помилки для кінцевого користувача,
 * для автоматичного оброблення потрібно аналізувати HTTP код відповіді (200, 404, 429 та інші)
 */
@Serializable
data class MonoErrorResponse(
    val errorDescription: String,
) : IErrorFormattable {
    override fun formatError() = errorDescription
}

@Serializable
data class MonoStatusResponse(
    val status: String,
)

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

/**
 * Опис клієнта та його рахунків.
 * Якщо клієнт не надав права читати його персональні данні, повернеться тільки перелік рахунків.
 */
@Serializable
data class MonoUserInfo(
    val clientId: String,
    val name: String,
    val webHookUrl: String,
    val accounts: List<MonoAccount>,
)

@Serializable
data class MonoWebHookRequest(
    val webHookUrl: String,
)

@Serializable
data class MonoWebhookResponse(
    val type: String,
    val data: MonoWebHookResponseData,
)

@Serializable
data class MonoWebHookResponseData(
    val account: MonoAccountId,
    val statementItem: MonoStatementItem,
)
