package com.github.smaugfm.ynab

import com.github.smaugfm.util.makeJson
import com.github.smaugfm.util.requestCatching
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import mu.KotlinLogging
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger { }

class YnabApi(
    private val token: String,
    private val budgetId: String,
) {
    private val json = makeJson()
    private val jsonSerializer = KotlinxSerializer(json)
    private val httpClient = HttpClient {
        install(JsonFeature) {
            serializer = jsonSerializer
        }
    }

    private fun url(vararg path: String): String =
        with(
            URLBuilder(
                URLProtocol.HTTPS,
                "api.youneedabudget.com",
                parameters = ParametersBuilder().also {
                    it.append("access_token", token)
                }
            )
        ) {
            path(listOf("v1") + path)
        }.buildString()

    private suspend inline fun <reified T : Any> catching(
        method: KFunction<Any>,
        block: () -> T,
    ): T = requestCatching("YNAB", logger, method.name, json, block)

    suspend fun getPayees(): List<YnabPayee> =
        catching(this::getPayees) {
            httpClient.get<YnabPayeesResponse>(
                url("budgets", budgetId, "payees")
            )
        }.data.payees

    suspend fun getCategories(): List<YnabCategoryGroupWithCategories> =
        catching(this::getCategories) {
            httpClient.get<YnabCategoriesResponse>(
                url("budgets", budgetId, "categories")
            )
        }.data.category_groups

    suspend fun createTransaction(
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        catching(this::createTransaction) {
            httpClient.post<YnabSaveTransactionResponse>(
                url("budgets", budgetId, "transactions")
            ) {
                body = jsonSerializer.write(YnabSaveTransactionWrapper(transaction))
            }
        }.data.transaction

    suspend fun updateTransaction(
        transactionId: String,
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        catching(this::updateTransaction) {
            httpClient.put<YnabTransactionResponseWithServerKnowledge>(
                url(
                    "budgets",
                    budgetId,
                    "transactions",
                    transactionId
                )
            ) {
                body = jsonSerializer.write(YnabSaveTransactionWrapper(transaction))
            }
        }.data.transaction

    suspend fun getTransaction(
        transactionId: String,
    ): YnabTransactionDetail =
        catching(this::getTransaction) {
            httpClient.get<YnabTransactionResponse>(
                url(
                    "budgets",
                    budgetId,
                    "transactions",
                    transactionId
                )
            )
        }.data.transaction

    suspend fun getAccountTransactions(
        accountId: String,
    ): List<YnabTransactionDetail> =
        catching(this::getAccountTransactions) {
            httpClient.get<YnabTransactionsResponse>(
                url(
                    "budgets",
                    budgetId,
                    "accounts",
                    accountId,
                    "transactions"
                )
            )
        }.data.transactions
}
