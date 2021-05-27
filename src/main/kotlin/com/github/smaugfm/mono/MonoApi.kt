package com.github.smaugfm.mono

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.util.makeJson
import com.github.smaugfm.util.requestCatching
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.defaultSerializer
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.features.ContentNegotiation
import io.ktor.features.origin
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.request.uri
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.StringFormat
import mu.KotlinLogging
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger {}
private val json = makeJson()

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

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
    ): T = requestCatching<T, MonoErrorResponse>("Monobank", logger, method.name, json, block)

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
                    logger.info("Webhook setup successful: $url")
                    waitForWebhook.complete(Unit)
                }
            }
        }
        logger.info("Starting webhook setup server...")
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

        @OptIn(DelicateCoroutinesApi::class)
        fun startMonoWebhookServerAsync(
            context: CoroutineContext,
            webhook: URI,
            port: Int,
            dispatcher: IEventDispatcher,
        ): Job {
            val server = embeddedServer(Netty, port = port) {
                install(ContentNegotiation) {
                    serialization(ContentType.Application.Json, json as StringFormat)
                }
                routing {
                    post(webhook.path) {
                        logger.info(
                            "Webhook queried. " +
                                "Host: ${call.request.origin.remoteHost} " +
                                "Uri: ${call.request.uri}"
                        )
                        val response = call.receive<MonoWebhookResponse>()
                        call.respond(HttpStatusCode.OK, "OK")
                        dispatcher(Event.Mono.WebHookQueried(response.data))
                    }
                }
            }
            return GlobalScope.launch(context) {
                server.start(wait = true)
            }
        }

        suspend fun Collection<MonoApi>.setupWebhookAll(webhook: URI, port: Int): Boolean =
            this.all { it.setWebHook(webhook, port) }
    }
}
