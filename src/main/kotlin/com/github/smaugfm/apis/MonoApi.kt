package com.github.smaugfm.apis

import com.github.smaugfm.models.MonoUserInfo
import com.github.smaugfm.models.MonoWebHookRequest
import com.github.smaugfm.util.logError
import com.github.smaugfm.util.makeJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.logging.error
import kotlinx.coroutines.CompletableDeferred
import mu.KotlinLogging
import java.net.URI
import java.net.URL
import kotlin.reflect.KFunction

private val logger = KotlinLogging.logger {}

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

    private val json = makeJson()

    private val httpClient = HttpClient {
        defaultRequest {
            header("X-Token", token)
        }
        install(ContentNegotiation) {
            json(makeJson())
        }
    }

    private suspend inline fun <reified T : Any> catching(
        method: KFunction<Any>,
        block: () -> T,
    ): T = logError("Monobank", logger, method.name, json, block) {
        // do nothing
    }

    suspend fun setWebHook(url: URI, port: Int): Boolean {
        require(url.toASCIIString() == url.toString())

        val waitForWebhook = CompletableDeferred<Unit>()
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
                httpClient.post {
                    url(buildUrl("personal/webhook"))
                    setBody(MonoWebHookRequest(url.toString()))
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

    companion object {
        private const val serverStopGracePeriod = 100L
        private fun buildUrl(endpoint: String) = URL("https://api.monobank.ua/$endpoint")
    }
}
