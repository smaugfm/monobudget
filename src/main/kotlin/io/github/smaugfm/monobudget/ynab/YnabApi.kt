package io.github.smaugfm.monobudget.ynab

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.common.util.logError
import io.github.smaugfm.monobudget.common.util.makeJson
import io.github.smaugfm.monobudget.ynab.model.YnabAccount
import io.github.smaugfm.monobudget.ynab.model.YnabAccountResponse
import io.github.smaugfm.monobudget.ynab.model.YnabAccountsResponse
import io.github.smaugfm.monobudget.ynab.model.YnabBudgetDetailResponseShort
import io.github.smaugfm.monobudget.ynab.model.YnabBudgetDetailShort
import io.github.smaugfm.monobudget.ynab.model.YnabCategoriesResponse
import io.github.smaugfm.monobudget.ynab.model.YnabCategoryGroupWithCategories
import io.github.smaugfm.monobudget.ynab.model.YnabPayee
import io.github.smaugfm.monobudget.ynab.model.YnabPayeesResponse
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransactionResponse
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransactionWrapper
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.util.url
import org.koin.core.annotation.Single
import kotlin.reflect.KFunction

private val log = KotlinLogging.logger { }

@Single
@Suppress("TooManyFunctions")
class YnabApi(backend: YNAB) {
    private val token = backend.token
    private val budgetId = backend.ynabBudgetId

    private val json = makeJson(true)
    private val httpClient =
        HttpClient {
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

    private inline fun <reified T : Any> catching(
        method: KFunction<Any>,
        block: () -> T,
    ): T =
        logError("YNAB", log, method.name, block) {
            if (it.response.status.value == HttpStatusCode.TooManyRequests.value) {
                throw YnabRateLimitException()
            }
        }

    private inline fun <reified T : Any> catchingNoLogging(
        method: KFunction<Any>,
        block: () -> T,
    ): T =
        logError("YNAB", null, method.name, block) {
            if (it.response.status.value == HttpStatusCode.TooManyRequests.value) {
                throw YnabRateLimitException()
            }
        }

    suspend fun getBudget(budgetId: String): YnabBudgetDetailShort =
        catching(this::getBudget) {
            httpClient.get(buildUrl("budgets", budgetId))
                .body<YnabBudgetDetailResponseShort>()
        }.data

    suspend fun getAccount(accountId: String): YnabAccount =
        catching(this::getAccount) {
            httpClient.get(buildUrl("budgets", budgetId, "accounts", accountId))
                .body<YnabAccountResponse>()
        }.data.account

    @Suppress("MemberVisibilityCanBePrivate", "unused")
    suspend fun getAccounts(): List<YnabAccount> =
        catching(this::getAccounts) {
            httpClient.get(buildUrl("budgets", budgetId, "accounts"))
                .body<YnabAccountsResponse>()
        }.data.accounts

    suspend fun getPayees(): List<YnabPayee> =
        catchingNoLogging(this::getPayees) {
            httpClient.get(buildUrl("budgets", budgetId, "payees"))
                .body<YnabPayeesResponse>()
        }.data.payees

    suspend fun getCategoryGroups(): List<YnabCategoryGroupWithCategories> =
        catching(this::getCategoryGroups) {
            httpClient.get(buildUrl("budgets", budgetId, "categories"))
                .body<YnabCategoriesResponse>()
        }.data.categoryGroups

    suspend fun createTransaction(transaction: YnabSaveTransaction): YnabTransactionDetail =
        catching(this::createTransaction) {
            httpClient.post(buildUrl("budgets", budgetId, "transactions")) {
                contentType(ContentType.Application.Json)
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
                    transactionId,
                ),
            ) {
                contentType(ContentType.Application.Json)
                setBody(YnabSaveTransactionWrapper(transaction))
            }.body<YnabTransactionResponse>()
        }.data.transaction

    suspend fun getTransaction(transactionId: String): YnabTransactionDetail =
        catching(this::getTransaction) {
            httpClient.get(
                buildUrl(
                    "budgets",
                    budgetId,
                    "transactions",
                    transactionId,
                ),
            ).body<YnabTransactionResponse>()
        }.data.transaction
}
