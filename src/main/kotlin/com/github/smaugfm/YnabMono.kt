package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.MonoApi.Companion.setupWebhook
import com.github.smaugfm.mono.model.MonoWebHookResponseData
import com.github.smaugfm.mono.model.MonoWebhookResponse
import com.github.smaugfm.processing.Event
import com.github.smaugfm.processing.EventsProcessor
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.telegram.startTelegramServer
import com.github.smaugfm.ynab.YnabApi
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class YnabMono : CliktCommand() {
    val dontSetWebhook by option().flag(default = true)
    val settings by option("--settings").convert { Settings.load(Paths.get(it)) }.required()

    private val serversCoroutinesContext = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    override fun run() {
        runBlocking {
            val monoApis = settings.monoTokens.map(::MonoApi)
            YnabApi(settings.ynabToken).use { ynabApi ->
                val eventsProcessor = EventsProcessor(ynabApi, emptyList())

                val (telegramServerJob, telegram) = startTelegramServer(
                    serversCoroutinesContext,
                    settings.telegramToken
                ) {
                    eventsProcessor.onNewEvent(it)
                }

                if (!dontSetWebhook)
                    monoApis.setupWebhook(settings.webhookURI)

                val monoWebhookServer =
                    MonoApi.startMonoWebhookServer(serversCoroutinesContext, settings.webhookURI) {
                        eventsProcessor.onNewEvent(Event.NewStatement(it))
                    }

                telegramServerJob.join()
                monoWebhookServer.join()
            }
        }
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)