package com.github.smaugfm.ynab

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString

class YnabApi(
    private val token: String,
    private val budgetId: String,
) {
    private val json = kotlinx.serialization.json.Json {
        prettyPrint = true
    }
    private val jsonSerializer = KotlinxSerializer(json)
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = jsonSerializer
        }
    }

    private fun url(vararg path: String): String =
        with(URLBuilder(
            URLProtocol.HTTPS,
            "api.youneedabudget.com",
            parameters = ParametersBuilder().also {
                it.append("access_token", token)
            })) {
            path(listOf("v1") + path)
        }.buildString()

    private suspend inline fun <reified T> requestCatching(block: () -> T): T {
        return try {
            block()
        } catch (e: ResponseException) {
            val error = json.decodeFromString<YnabErrorResponse>(e.response.readText())
            throw YnabApiException(e, error)
        }
    }

    suspend fun createTransaction(
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        requestCatching {
            httpClient.post<YnabSaveTransactionResponse>(url("budgets", budgetId, "transactions")) {
                body = jsonSerializer.write(YnabSaveTransactionWrapper(transaction))
            }
        }.data.transaction

    suspend fun updateTransaction(
        transactionId: String,
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        requestCatching {
            httpClient.put<YnabTransactionResponseWithServerKnowledge>(url("budgets",
                budgetId,
                "transactions",
                transactionId)) {
                body = jsonSerializer.write(YnabSaveTransactionWrapper(transaction))
            }
        }.data.transaction

    suspend fun getTransaction(
        transactionId: String,
    ): YnabTransactionDetail =
        requestCatching {
            httpClient.get<YnabTransactionResponse>(url("budgets",
                budgetId,
                "transactions",
                transactionId))
        }.data.transaction

    suspend fun getAccountTransactions(
        accountId: String,
    ): List<YnabTransactionDetail> =
        requestCatching {
            httpClient.get<YnabTransactionsResponse>(url("budgets",
                budgetId,
                "accounts",
                accountId,
                "transactions"))
        }.data.transactions
}

