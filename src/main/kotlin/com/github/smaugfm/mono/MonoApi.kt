package com.github.smaugfm.mono

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEventDispatcher
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.ResponseException
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.readText
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.URI
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

    private var previousStatementCallTimestamp = Long.MIN_VALUE / 2

    private val httpClient = HttpClient {
        defaultRequest {
            header("X-Token", token)
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer()
        }
    }

    suspend fun fetchUserInfo(): MonoUserInfo {
        val infoString = httpClient.get<String>(url("personal/client-info"))
        return Json.decodeFromString(infoString)
    }

    suspend fun setWebHook(url: URI, port: Int): MonoStatusResponse {
        require(url.toASCIIString() == url.toString())

        val waitForWebhook = CompletableDeferred<Unit>()
        val json = defaultSerializer()
        val tempServer = embeddedServer(Netty, port = port) {
            routing {
                get(url.path) {
                    call.response.status(HttpStatusCode.OK)
                    call.respondText("OK\n", ContentType.Text.Plain)
                    logger.info("Webhook setup successful: $url")
                    waitForWebhook.complete(Unit)
                }
            }
        }
        logger.info("Starting webhook setup server...")
        tempServer.start(wait = false)

        val statusString = try {
            httpClient.post<String>(url("personal/webhook")) {
                body = json.write(MonoWebHookRequest(url.toString()))
            }
        } catch (e: ResponseException) {
            logger.error(e.response.readText())
            throw e
        }
        waitForWebhook.await()
        tempServer.stop(serverStopGracePeriod, serverStopGracePeriod)
        return Json.decodeFromString(statusString)
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

        val itemsString =
            httpClient.get<String>(url("personal/statement/$id/${from.epochSeconds}/${to.epochSeconds}")).also {
                previousStatementCallTimestamp = System.currentTimeMillis()
            }

        return Json.decodeFromString(itemsString)
    }

    suspend fun fetchBankCurrency(): List<MonoCurrencyInfo> {
        val infoString = httpClient.get<String>(url("bank/currency"))
        return Json.decodeFromString(infoString)
    }

    companion object {
        private const val StatementCallRate = 60000
        private const val serverStopGracePeriod = 100L
        private fun url(endpoint: String) = "https://api.monobank.ua/$endpoint"

        fun startMonoWebhookServerAsync(
            context: CoroutineContext,
            webhook: URI,
            port: Int,
            dispatcher: IEventDispatcher,
        ): Job {
            val server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    json()
                }
                routing {
                    post(webhook.path) {
                        call.request.origin.host
                        logger.info(
                            "Webhook queried. " +
                                "Host: ${call.request.origin.remoteHost} " +
                                "Uri: ${call.request.uri}"
                        )
                        val response = call.receive<MonoWebhookResponse>()
                        call.response.status(HttpStatusCode.OK)
                        dispatcher(Event.Mono.NewStatementReceived(response.data))
                    }
                }
            }
            return GlobalScope.launch(context) {
                server.start(wait = true)
            }
        }

        suspend fun Collection<MonoApi>.setupWebhookAll(webhook: URI, port: Int) {
            for (it in this) {
                try {
                    it.setWebHook(webhook, port)
                } catch (e: Throwable) {
                    logger.error("Error setting webhook.", e)
                }
            }
        }
    }
}
