package com.github.smaugfm.apis

import com.github.smaugfm.ynab.*
import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*

class YnabApi(
    private val token: String,
    private val budgetId: String,
) {
    private val json = defaultSerializer()
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = KotlinxSerializer()
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
        }.toString()

    suspend fun createTransaction(
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        httpClient.post<YnabSaveTransactionResponse>(url("budgets", budgetId, "transactions")) {
            body = json.write(YnabSaveTransactionWrapper(transaction))
        }.data.transaction

    suspend fun updateTransaction(
        transactionId: String,
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        httpClient.put<YnabTransactionResponse>(url("budgets", budgetId, "transactions", transactionId)) {
            body = json.write(YnabSaveTransactionWrapper(transaction))
        }.data.transaction

    suspend fun getTransaction(
        transactionId: String,
    ): YnabTransactionDetail =
        httpClient.get<YnabTransactionResponse>(url("budgets",
            budgetId,
            "transactions",
            transactionId)).data.transaction
}