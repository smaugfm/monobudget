package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.events.EventProcessor
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.wrappers.MonoApi
import com.github.smaugfm.wrappers.MonoApi.Companion.setupWebhook
import com.github.smaugfm.wrappers.TelegramApi
import com.github.smaugfm.wrappers.YnabApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths
import java.util.concurrent.Executors

class YnabMono : CliktCommand() {
    val dontSetWebhook by option().flag(default = true)
    val settings by option("--settings").convert { Settings.load(Paths.get(it)) }.required()

    private val serversCoroutinesContext = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    override fun run() {
        runBlocking {
            val monoApis = settings.monoTokens.map(::MonoApi)
            if (!dontSetWebhook)
                monoApis.setupWebhook(settings.webhookURI)

            val events = EventProcessor(
                settings.mono2Ynab,
                settings.telegram2Mono,
                listOf(
                    Handlers::telegramEventReceived,
                    Handlers::newStatementReceivedHandler,
                ),
            )

            YnabApi(settings.ynabToken).use { ynabApi ->
                val (telegramServer, telegramApi) = TelegramApi.startTelegramServerAsync(
                    serversCoroutinesContext,
                    settings.telegramToken,
                    events::dispatch
                )
                val monoWebhookServer =
                    MonoApi.startMonoWebhookServerAsync(
                        serversCoroutinesContext,
                        settings.webhookURI,
                        events::dispatch,
                    )

                events.init(ynabApi, telegramApi)

                telegramServer.await()
                monoWebhookServer.await()
            }
        }
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)