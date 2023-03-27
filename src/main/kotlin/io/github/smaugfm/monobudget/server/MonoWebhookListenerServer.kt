package io.github.smaugfm.monobudget.server

import io.github.smaugfm.monobank.model.MonoWebhookResponse
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.makeJson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.serialization
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.receive
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI

private val log = KotlinLogging.logger {}

@Suppress("ExtractKtorModule")
class MonoWebhookListenerServer : KoinComponent {
    private val scope: CoroutineScope by inject()
    private val monoSettings: Settings.MultipleMonoSettings by inject()

    private val json = makeJson()

    suspend fun setupWebhook(monoWebhookUrl: URI, webhookPort: Int) =
        monoSettings.apis.all { it.setupWebhook(monoWebhookUrl, webhookPort) }

    fun start(webhook: URI, port: Int, callback: suspend (MonoWebhookResponseData) -> Unit): Job {
        val server = scope.embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                serialization(ContentType.Application.Json, json)
            }
            routing {
                post(webhook.path) {
                    log.info(
                        "Webhook queried. " +
                            "Host: ${call.request.origin.remoteHost} " +
                            "Uri: ${call.request.uri}"
                    )
                    val response = call.receive<MonoWebhookResponse>()
                    try {
                        callback(
                            MonoWebhookResponseData(
                                response.data.account,
                                response.data.statementItem
                            )
                        )
                    } finally {
                        call.respond(HttpStatusCode.OK, "OK")
                    }
                }
            }
        }

        return scope.launch(context = Dispatchers.IO) {
            server.start(true)
        }
    }
}
