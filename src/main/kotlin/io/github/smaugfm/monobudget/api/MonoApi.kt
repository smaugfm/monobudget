@file:OptIn(ExperimentalSerializationApi::class)

package io.github.smaugfm.monobudget.api

import io.github.smaugfm.monobank.MonobankPersonalApi
import io.github.smaugfm.monobudget.util.buildJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.util.logging.error
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonBuilder
import mu.KotlinLogging
import java.net.URI

private val logger = KotlinLogging.logger {}

class MonoApi(private val token: String) {
    init {
        require(token.isNotBlank())
    }

    private val api = MonobankPersonalApi(token, JsonBuilder::buildJson)

    @Suppress("ExtractKtorModule")
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
            api.setClientWebhook(url.toString()).awaitSingle()
            waitForWebhook.await()
            logger.debug { "Webhook setup completed. Stopping temporary server..." }
        } catch (e: Throwable) {
            logger.error(e)
            return false
        } finally {
            tempServer.stop(serverStopGracePeriod, serverStopGracePeriod)
        }

        return true
    }

    companion object {
        private const val serverStopGracePeriod = 100L
    }
}
