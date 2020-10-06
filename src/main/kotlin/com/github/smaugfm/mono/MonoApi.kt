package com.github.smaugfm.mono

import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import com.github.smaugfm.mono.model.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

    companion object {
        private fun url(endpoint: String) = "https://api.monobank.ua/${endpoint}"
    }

    private val httpClient = HttpClient {
        defaultRequest {
            header("X-Token", token)
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend fun fetchUserInfo(): UserInfo {
        val infoString = httpClient.get<String>(url("personal/client-info"))
        return Json.decodeFromString(infoString)
    }

    suspend fun setWebHook(urlString: String): Status {
        val json = defaultSerializer()
        val statusString = httpClient.post<String>(url("personal/webhook")) {
            body = json.write(WebHookRequest(urlString))
        }
        return Json.decodeFromString(statusString)
    }

    suspend fun fetchStatementItems(
        id: AccountId,
        from: Instant,
        to: Instant = Clock.System.now()
    ): List<StatementItem> {
        val itemsString =
            httpClient.get<String>(url("personal/statement/$id/${from.epochSeconds}/${to.epochSeconds}"))
        return Json.decodeFromString(itemsString)
    }

    suspend fun fetchBankCurrency(): List<CurrencyInfo> {
        val infoString = httpClient.get<String>(url("bank/currency"))
        return Json.decodeFromString(infoString)
    }
}