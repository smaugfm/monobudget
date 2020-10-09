package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.model.MonoWebhookResponse
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.ynab.YnabApi
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.nio.file.Paths

class YnabMono : CliktCommand() {
    val ynabToken by option().required()
    val monoTokens by option("--mono-token").multiple(required = true).unique()
    val webhook by option().convert { URI(it) }.required()
    val dontSetWebhook by option().flag(default = true)
    val settings by option("--settings").convert { Settings.load(Paths.get(it)) }.required()

    override fun run() {
        require(monoTokens.isNotEmpty())

        val monoApis = monoTokens.map(::MonoApi)
        setupWebhook(monoApis)

        YnabApi(ynabToken).use { ynabApi ->
            runBlocking {
                println(ynabApi.getAccounts(settings.ynabBudgetIt))
                monoApis.forEach {
                    println(it.fetchUserInfo().accounts)
                }
            }
        }
    }

    private fun setupWebhook(apis: List<MonoApi>) {
        if (!dontSetWebhook) {
            runBlocking {
                apis.forEach {
                    it.setWebHook(webhook)
                }
            }
        }

    }

    private fun startWebhookServer() {
        val server = embeddedServer(Netty, port = webhook.port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                post(webhook.path) {
                    val response = call.receive<MonoWebhookResponse>();
                    println(response)
                    call.response.status(HttpStatusCode.OK)
                    call.respondText("OK\n", ContentType.Text.Plain)
                }
            }
        }
        server.start(wait = true)
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)