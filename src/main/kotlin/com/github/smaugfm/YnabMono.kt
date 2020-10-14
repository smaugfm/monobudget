package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.events.EventsDispatcher
import com.github.smaugfm.handlers.MonoHandler
import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.handlers.YnabHandler
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.MonoApi.Companion.setupWebhook
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.ynab.YnabApi
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
            val telegramApi = TelegramApi.create(
                settings.telegramBotUsername,
                settings.telegramBotToken,
                settings.mappings.getTelegramChatIds()
            )
            val ynabApi = YnabApi(settings.ynabToken, settings.ynabBudgetId)

            if (!dontSetWebhook)
                monoApis.setupWebhook(settings.webhookURI)

            val events = EventsDispatcher(
                MonoHandler(settings.mappings),
                YnabHandler(ynabApi, settings.mappings),
                TelegramHandler(telegramApi, settings.mappings)
            )

            val telegramServerJob = telegramApi.startServer(serversCoroutinesContext, events::dispatch)
            val monoWebhookServer =
                MonoApi.startMonoWebhookServerAsync(
                    serversCoroutinesContext,
                    settings.webhookURI,
                    events::dispatch,
                )

            telegramServerJob.join()
            monoWebhookServer.join()
        }
    }
}

fun main(args: Array<String>) =
    YnabMono().main(args)
