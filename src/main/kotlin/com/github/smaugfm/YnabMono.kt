package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.mono.MonoApi
import io.ktor.application.*
import io.ktor.client.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import java.io.InputStreamReader
import java.net.URI
import kotlin.system.exitProcess
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class YnabMono() : CliktCommand() {
    val token by option().required()
    val webHookUrl by option().required()

    override fun run() {
        val url = URI(webHookUrl)
        runBlocking {
            launch {
                val server = embeddedServer(Netty, port = url.port) {
                    routing {
                        get(url.path) {
                            call.response.status(HttpStatusCode.OK)
                            call.respondText("OK\n", ContentType.Text.Plain)

                            println("WebHook setup successful.")
                            exitProcess(0)
                        }
                    }
                }
                server.start(wait = true)
            }
            val api = MonoApi(token)

            try {
                api.setWebHook(webHookUrl)
            } catch (e: ClientRequestException) {
                println(InputStreamReader(e.response.content.toInputStream()).readText())
            }
        }
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)