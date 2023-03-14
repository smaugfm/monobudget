package com.github.smaugfm.apis

import com.github.smaugfm.models.MonoWebHookResponseData
import com.github.smaugfm.models.MonoWebhookResponse
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.makeJson
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
import kotlinx.serialization.StringFormat
import mu.KotlinLogging
import java.net.URI

private val logger = KotlinLogging.logger {}

class MonoApis(
    private val scope: CoroutineScope,
    settings: Settings
) {
    val apis = settings.monoTokens.map(::MonoApi)
    private val json = makeJson()

    fun listenWebhooks(
        webhook: URI,
        port: Int,
        callback: suspend (MonoWebHookResponseData) -> Unit,
    ): Job {
        val server = scope.embeddedServer(Netty, port = port) {
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
                    callback(
                        MonoWebHookResponseData(
                            response.data.account,
                            response.data.statementItem
                        )
                    )
                    call.respond(HttpStatusCode.OK, "OK")
                }
            }
        }

        return scope.launch(context = Dispatchers.IO) {
            server.start(true)
        }
    }
}
