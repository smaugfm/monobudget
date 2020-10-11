package com.github.smaugfm.apis

import com.github.smaugfm.ynab.YnabSaveTransaction
import com.github.smaugfm.ynab.YnabSaveTransactionWrapper
import com.github.smaugfm.ynab.YnabTransactionResponse
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
    ): YnabTransactionResponse = httpClient.post(url("budgets", budgetId, "transactions")) {
        body = json.write(YnabSaveTransactionWrapper(transaction))
    }
}