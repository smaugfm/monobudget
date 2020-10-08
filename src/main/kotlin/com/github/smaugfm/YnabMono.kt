package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.model.WebHookResponse
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
import ynab.client.api.BudgetsApi
import ynab.client.invoker.ApiClient
import ynab.client.invoker.Configuration
import ynab.client.invoker.auth.ApiKeyAuth
import java.net.URI


class YnabMono() : CliktCommand() {
    val ynabToken by option().required()
    val monoToken by option().required()
    val webhook by option().required()
    val setWebhook by option().flag()

    override fun run() {
        val api = MonoApi(monoToken)
        YnabApi(ynabToken).use { ynabApi ->
            runBlocking {
                println(ynabApi.getBudgets())
            }
        }
    }

    private suspend fun printAccounts(api: MonoApi) {
        api.fetchUserInfo().accounts.forEach {
            with(it) {
                println("Account:")
                println("\tid: $id")
                println("\tcurrency: $currencyCode")
                println("\tbalance: ${balance / 100.0}")
                println("\ttype: $type")
                println()
            }
        }
    }

    private suspend fun setupWebhook(api: MonoApi) {
        val url = URI(webhook)
        if (setWebhook) {

            api.setWebHook(url)
        }

        val server = embeddedServer(Netty, port = url.port) {
            install(ContentNegotiation) {
                json()
            }
            routing {
                post(url.path) {
                    val response = call.receive<WebHookResponse>();
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