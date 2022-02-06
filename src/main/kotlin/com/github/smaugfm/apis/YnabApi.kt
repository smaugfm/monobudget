package com.github.smaugfm.apis

import com.github.smaugfm.models.YnabAccount
import com.github.smaugfm.models.YnabAccountResponse
import com.github.smaugfm.models.YnabAccountsResponse
import com.github.smaugfm.models.YnabCategoriesResponse
import com.github.smaugfm.models.YnabCategoryGroupWithCategories
import com.github.smaugfm.models.YnabErrorResponse
import com.github.smaugfm.models.YnabPayee
import com.github.smaugfm.models.YnabPayeesResponse
import com.github.smaugfm.models.YnabSaveTransaction
import com.github.smaugfm.models.YnabSaveTransactionResponse
import com.github.smaugfm.models.YnabSaveTransactionWrapper
import com.github.smaugfm.models.YnabTransactionDetail
import com.github.smaugfm.models.YnabTransactionResponse
import com.github.smaugfm.models.YnabTransactionResponseWithServerKnowledge
import com.github.smaugfm.models.YnabTransactionsResponse
import com.github.smaugfm.models.settings.Settings
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

class YnabApi(settings: Settings) {
    private val token = settings.ynabToken
    private val budgetId = settings.ynabBudgetId

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
    ): T = requestCatching<T, YnabErrorResponse>("YNAB", logger, method.name, json, block)

    private suspend inline fun <reified T : Any> catchingNoLogging(
        method: KFunction<Any>,
        block: () -> T,
    ): T = requestCatching<T, YnabErrorResponse>("YNAB", null, method.name, json, block)

    suspend fun getAccounts(): List<YnabAccount> =
        catching(this::getAccounts) {
            httpClient.get<YnabAccountsResponse>(
                url("budgets", budgetId, "accounts")
            )
        }.data.accounts

    suspend fun getAccount(accountId: String): YnabAccount =
        catching(this::getAccount) {
            httpClient.get<YnabAccountResponse>(
                url("budgets", budgetId, "accounts", accountId)
            )
        }.data.account

    suspend fun getPayees(): List<YnabPayee> =
        catchingNoLogging(this::getPayees) {
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
