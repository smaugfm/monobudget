package com.github.smaugfm.api

import com.github.smaugfm.models.ynab.YnabAccount
import com.github.smaugfm.models.ynab.YnabAccountResponse
import com.github.smaugfm.models.ynab.YnabPayee
import com.github.smaugfm.models.ynab.YnabPayeesResponse
import com.github.smaugfm.models.ynab.YnabSaveTransaction
import com.github.smaugfm.models.ynab.YnabSaveTransactionResponse
import com.github.smaugfm.models.ynab.YnabSaveTransactionWrapper
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.models.ynab.YnabTransactionResponse
import com.github.smaugfm.models.ynab.YnabTransactionResponseWithServerKnowledge
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.YnabRateLimitException
import com.github.smaugfm.util.logError
import com.github.smaugfm.util.makeJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.util.url
import mu.KotlinLogging
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger { }

class YnabApi(settings: Settings) {
    private val token = settings.ynabToken
    private val budgetId = settings.ynabBudgetId

    private val json = makeJson(true)
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun buildUrl(vararg path: String): String =
        url {
            protocol = URLProtocol.HTTPS
            host = "api.youneedabudget.com"
            parameters.append("access_token", token)
            path("v1", *path)
        }

    private suspend inline fun <reified T : Any> catching(
        method: KFunction<Any>,
        block: () -> T,
    ): T = logError("YNAB", logger, method.name, json, block) {
        if (it.response.status.value == 429) {
            throw YnabRateLimitException()
        }
    }

    private suspend inline fun <reified T : Any> catchingNoLogging(
        method: KFunction<Any>,
        block: () -> T,
    ): T = logError("YNAB", null, method.name, json, block) {
        if (it.response.status.value == 429) {
            throw YnabRateLimitException()
        }
    }

    suspend fun getAccount(accountId: String): YnabAccount =
        catching(this::getAccount) {
            httpClient.get(buildUrl("budgets", budgetId, "accounts", accountId))
                .body<YnabAccountResponse>()
        }.data.account

    suspend fun getPayees(): List<YnabPayee> =
        catchingNoLogging(this::getPayees) {
            httpClient.get(buildUrl("budgets", budgetId, "payees"))
                .body<YnabPayeesResponse>()
        }.data.payees

    suspend fun createTransaction(
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        catching(this::createTransaction) {
            httpClient.post(buildUrl("budgets", budgetId, "transactions")) {
                setBody(YnabSaveTransactionWrapper(transaction))
            }.body<YnabSaveTransactionResponse>()
        }.data.transaction

    suspend fun updateTransaction(
        transactionId: String,
        transaction: YnabSaveTransaction,
    ): YnabTransactionDetail =
        catching(this::updateTransaction) {
            httpClient.put(
                buildUrl(
                    "budgets",
                    budgetId,
                    "transactions",
                    transactionId
                )
            ) {
                setBody(YnabSaveTransactionWrapper(transaction))
            }.body<YnabTransactionResponseWithServerKnowledge>()
        }.data.transaction

    suspend fun getTransaction(
        transactionId: String,
    ): YnabTransactionDetail =
        catching(this::getTransaction) {
            httpClient.get(
                buildUrl(
                    "budgets",
                    budgetId,
                    "transactions",
                    transactionId
                )
            ).body<YnabTransactionResponse>()
        }.data.transaction
}
