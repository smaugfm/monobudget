package com.github.smaugfm.wrappers

import kotlinx.coroutines.CompletableDeferred
import ynab.client.api.AccountsApi
import ynab.client.api.BudgetsApi
import ynab.client.invoker.ApiCallback
import ynab.client.invoker.ApiClient
import ynab.client.invoker.ApiException
import ynab.client.invoker.Configuration
import ynab.client.invoker.auth.ApiKeyAuth
import ynab.client.model.AccountsResponse
import ynab.client.model.BudgetSummaryResponse

class YnabApi(token: String) : AutoCloseable {
    init {
        val defaultClient: ApiClient = Configuration.getDefaultApiClient()

        val bearer = defaultClient.getAuthentication("bearer") as ApiKeyAuth
        bearer.apiKey = token
        bearer.apiKeyPrefix = "Bearer"
    }

    private fun <T> asyncAdapter(deferred: CompletableDeferred<T>): ApiCallback<T> {
        return object : ApiCallback<T> {
            override fun onSuccess(
                result: T,
                statusCode: Int,
                responseHeaders: MutableMap<String, MutableList<String>>?,
            ) {
                deferred.complete(result)
            }

            override fun onFailure(
                e: ApiException?,
                statusCode: Int,
                responseHeaders: MutableMap<String, MutableList<String>>?,
            ) {
                deferred.completeExceptionally(e ?: ApiException(statusCode, "Unknown error"))
            }

            override fun onUploadProgress(bytesWritten: Long, contentLength: Long, done: Boolean) {
            }

            override fun onDownloadProgress(bytesRead: Long, contentLength: Long, done: Boolean) {
            }
        }
    }

    suspend fun getAccounts(budgetId: String): AccountsResponse {
        val deferred = CompletableDeferred<AccountsResponse>()
        AccountsApi().getAccountsAsync(budgetId, asyncAdapter(deferred))

        return deferred.await()
    }

    suspend fun getBudgets(): BudgetSummaryResponse {

        val deferred = CompletableDeferred<BudgetSummaryResponse>()
        BudgetsApi().getBudgetsAsync(asyncAdapter(deferred))

        return deferred.await()
    }

    override fun close() {
        Configuration.getDefaultApiClient().httpClient.dispatcher.executorService.shutdownNow()
    }
}