package io.github.smaugfm.monobudget.mono

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobank.model.MonoWebhookResponse
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.common.util.injectAll
import io.github.smaugfm.monobudget.common.util.makeJson
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

@Suppress("ExtractKtorModule")
@Single
class MonoWebhookListener : StatementSource, KoinComponent {
    private val scope: CoroutineScope by inject()
    private val monoAccountsService: MonoAccountsService by inject()
    private val settings: MonoWebhookSettings by inject()
    private val monoApis by injectAll<MonoApi>()

    private val json = makeJson()

    override suspend fun prepare() {
        if (settings.setWebhook) {
            log.info { "Setting up mono webhooks." }
            try {
                monoApis.forEach { it.setupWebhook(settings.monoWebhookUrl, settings.webhookPort) }
            } catch (e: Throwable) {
                log.error(e) {}
                error("Error settings up webhooks. Exiting application...")
            }
        } else {
            log.info { "Skipping mono webhook setup." }
        }
    }

    override suspend fun statements(): Flow<StatementProcessingContext> {
        val (_, monoWebhookUrl, webhookPort) = settings

        val flow = MutableSharedFlow<StatementProcessingContext>()
        val server =
            scope.embeddedServer(Netty, port = webhookPort) {
                install(ContentNegotiation) {
                    serialization(ContentType.Application.Json, json)
                }
                routing {
                    post(monoWebhookUrl.path) {
                        log.info {
                            "Webhook queried. " +
                                "Host: ${call.request.origin.remoteHost} " +
                                "Uri: ${call.request.uri}"
                        }
                        val response = call.receive<MonoWebhookResponse>()
                        try {
                            val account =
                                monoAccountsService.getAccounts()
                                    .firstOrNull { it.id == response.data.account }
                            if (account == null) {
                                log.error {
                                    "Skipping transaction from Monobank " +
                                        "accountId=${response.data.account} " +
                                        "because this account is not configured: $response"
                                }
                            } else {
                                flow.emit(
                                    StatementProcessingContext(
                                        MonobankWebhookResponseStatementItem(response.data, account.currency),
                                    ),
                                )
                            }
                        } finally {
                            call.respond(HttpStatusCode.OK, "OK")
                        }
                    }
                }
            }

        scope.launch(context = Dispatchers.IO) {
            server.start(true)
        }

        return flow
    }
}
