package io.github.smaugfm.monobudget.mono

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobank.MonobankPersonalApi
import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.reactor.awaitSingleOrNull
import java.net.URI

private val log = KotlinLogging.logger {}

class MonoApi(token: String, val accountId: BankAccountId, private val alias: String) {
    init {
        require(token.isNotBlank())
    }

    val api = MonobankPersonalApi(token)

    suspend fun setupWebhook(
        url: URI,
        port: Int,
    ) {
        require(url.toASCIIString() == url.toString())

        val waitForWebhook = CompletableDeferred<Unit>()
        val tempServer =
            embeddedServer(Netty, port = port) {
                routing {
                    get(url.path) {
                        call.response.status(HttpStatusCode.OK)
                        call.respondText("OK\n", ContentType.Text.Plain)
                        log.info { "Webhook setup for $alias successful: $url" }
                        waitForWebhook.complete(Unit)
                    }
                }
            }
        log.info { "Starting temporary webhook setup server..." }
        tempServer.start(wait = false)

        try {
            api.setClientWebhook(url.toString()).awaitSingleOrNull()
            waitForWebhook.await()
            log.info { "Webhook setup completed. Stopping temporary server..." }
        } finally {
            tempServer.stop(SERVER_STOP_GRACE_PERIOD, SERVER_STOP_GRACE_PERIOD)
        }
    }

    companion object {
        private const val SERVER_STOP_GRACE_PERIOD = 100L
    }
}
