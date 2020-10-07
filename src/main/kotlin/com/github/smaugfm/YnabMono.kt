package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.model.WebHookResponse
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ynab.client.api.BudgetsApi
import java.io.InputStreamReader
import java.net.URI
import kotlin.system.exitProcess

class YnabMono() : CliktCommand() {
    val token by option().required()
    val webhook by option().required()
    val setWebhook by option().flag()

    override fun run() {
        val api = MonoApi(token)
        runBlocking {
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