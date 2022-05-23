package com.github.smaugfm.apis

import com.github.smaugfm.models.MonoAccountId
import com.github.smaugfm.models.MonoCurrencyInfo
import com.github.smaugfm.models.MonoErrorResponse
import com.github.smaugfm.models.MonoStatementItem
import com.github.smaugfm.models.MonoStatusResponse
import com.github.smaugfm.models.MonoUserInfo
import com.github.smaugfm.models.MonoWebHookRequest
import com.github.smaugfm.util.logError
import com.github.smaugfm.util.makeJson
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import mu.KotlinLogging
import java.net.URI
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger {}

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

    private val json = makeJson()
    private var previousStatementCallTimestamp = Long.MIN_VALUE / 2
    private val jsonSerializer = KotlinxSerializer(json)

    private val httpClient = HttpClient {
        defaultRequest {
            header("X-Token", token)
        }
        install(JsonFeature) {
            serializer = jsonSerializer
        }
    }

    private suspend inline fun <reified T : Any> catching(
        method: KFunction<Any>,
        block: () -> T,
    ): T = logError<T, MonoErrorResponse>("Monobank", logger, method.name, json, block) {
        // do nothing
    }

    @Suppress("unused")
    suspend fun fetchUserInfo(): MonoUserInfo =
        catching(this::fetchUserInfo) { httpClient.get(url("personal/client-info")) }

    suspend fun setWebHook(url: URI, port: Int): Boolean {
        require(url.toASCIIString() == url.toString())

        val waitForWebhook = CompletableDeferred<Unit>()
        val json = defaultSerializer()
        val tempServer = embeddedServer(Netty, port = port) {
            routing {
                get(url.path) {
                    call.response.status(HttpStatusCode.OK)
                    call.respondText("OK\n", ContentType.Text.Plain)
                    logger.debug { "Webhook setup successful: $url" }
                    waitForWebhook.complete(Unit)
                }
            }
        }
        logger.debug { "Starting temporary webhook setup server..." }
        tempServer.start(wait = false)

        try {
            catching(this::setWebHook) {
                httpClient.post<MonoStatusResponse>(url("personal/webhook")) {
                    body = json.write(MonoWebHookRequest(url.toString()))
                }
            }
        } catch (e: Throwable) {
            logger.error(e)
            tempServer.stop(serverStopGracePeriod, serverStopGracePeriod)
            return false
        }
        waitForWebhook.await()
        logger.debug { "Webhook setup completed. Stopping temporary server..." }
        tempServer.stop(serverStopGracePeriod, serverStopGracePeriod)

        return true
    }

    suspend fun fetchStatementItems(
        id: MonoAccountId,
        from: Instant,
        to: Instant = Clock.System.now(),
    ): List<MonoStatementItem> {
        val currentTime = System.currentTimeMillis()
        if (currentTime - previousStatementCallTimestamp < StatementCallRate) {
            delay(StatementCallRate - (currentTime - previousStatementCallTimestamp))
        }

        return catching(this::fetchStatementItems) {
            httpClient.get<List<MonoStatementItem>>(
                url("personal/statement/$id/${from.epochSeconds}/${to.epochSeconds}")
            )
                .also {
                    previousStatementCallTimestamp = System.currentTimeMillis()
                }
        }
    }

    @Suppress("unused")
    suspend fun fetchBankCurrency(): List<MonoCurrencyInfo> =
        catching(this::fetchBankCurrency) { httpClient.get(url("bank/currency")) }

    companion object {
        private const val StatementCallRate = 60000
        private const val serverStopGracePeriod = 100L
        private fun url(endpoint: String) = "https://api.monobank.ua/$endpoint"
    }
}
