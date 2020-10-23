package com.github.smaugfm.ynab

import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.readText
import io.ktor.http.ParametersBuilder
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.serialization.decodeFromString
import mu.KotlinLogging
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger { }

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

    private suspend inline fun <reified T> requestCatching(
        method: KFunction<Any>,
        vararg args: Any,
        block: () -> T,
    ): T {
        return try {
            logger.info(
                "Performing YNAB request ${method.name}, " +
                    "args: ${args.joinToString(", ") { "\"it\"" }}"
            )
            block().also {
                logger.info("Request done ${method.name}. Response:\n\t$it")
            }
        } catch (e: ResponseException) {
            val error = json.decodeFromString<YnabErrorResponse>(e.response.readText())
            logger.info("Request failed ${method.name}. Error response:\n\t$error")
            throw YnabApiException(e, error)
        }
    }

    suspend fun getPayees(): List<YnabPayee> =
        requestCatching(this::getPayees) {
            httpClient.get<YnabPayeesResponse>(
                url("budgets", budgetId, "payees")
            )
        }.data.payees

    suspend fun getCategories(): List<YnabCategoryGroupWithCategories> =
        requestCatching(this::getCategories) {
            httpClient.get<YnabCategoriesResponse>(
                url("budgets", budgetId, "categories")
            )
        }.data.category_groups

    suspend fun createTransaction(
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        requestCatching(this::createTransaction) {
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
        requestCatching(this::updateTransaction) {
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
        requestCatching(this::getTransaction) {
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
        requestCatching(this::getAccountTransactions) {
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
