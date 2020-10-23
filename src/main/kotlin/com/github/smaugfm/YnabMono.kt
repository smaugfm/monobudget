package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.smaugfm.events.EventsDispatcherI
import com.github.smaugfm.mono.MonoApi
import com.github.smaugfm.mono.MonoApi.Companion.setupWebhook
import com.github.smaugfm.mono.MonoHandler
import com.github.smaugfm.settings.Settings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TelegramHandler
import com.github.smaugfm.ynab.YnabApi
import com.github.smaugfm.ynab.YnabHandler
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

class YnabMono : CliktCommand() {
    val dontSetWebhook by option().flag(default = true)
    val monoWebhookUrl by option().convert { URI(it) }.required()
    val telegramWebhookUrl
    by option().convert { URI(it).also { uri -> assert(uri.toString().startsWith("https")) } }
    val settings by option("--settings").convert { Settings.load(Paths.get(it)) }.required()

    private val serversCoroutinesContext = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

    override fun run() {
        logger.info(
            "Input arguments:\n\t" +
                "${this::settings.name}: $settings\n\t" +
                "${this::telegramWebhookUrl.name}: $telegramWebhookUrl\n\t" +
                "${this::monoWebhookUrl.name}: $monoWebhookUrl\n\t" +
                "${this::dontSetWebhook.name}: $dontSetWebhook",
        )

        try {
            runBlocking {
                val monoApis = settings.monoTokens.map(::MonoApi)
                logger.info("Created monobank apis. ")
                val telegramApi = TelegramApi(
                    settings.telegramBotUsername,
                    settings.telegramBotToken,
                    settings.mappings.getTelegramChatIds(),
                    telegramWebhookUrl
                )
                logger.info("Created telegram api.")
                val ynabApi = YnabApi(settings.ynabToken, settings.ynabBudgetId)
                logger.info("Created ynab api.")

                if (!dontSetWebhook) {
                    monoApis.setupWebhook(monoWebhookUrl)
                    logger.info("Mono webhook setup successful. $monoWebhookUrl")
                } else {
                    logger.info("Skipping mono webhook setup.")
                }

                val events = EventsDispatcherI(
                    MonoHandler(settings.mappings),
                    YnabHandler(ynabApi, settings.mappings),
                    TelegramHandler(telegramApi, settings.mappings)
                )

                logger.info("Events dispatcher created.")

                val telegramServerJob = telegramApi
                    .startServer(serversCoroutinesContext, events::dispatch)

                logger.info("Telegram bot started.")
                val monoWebhookServer =
                    MonoApi.startMonoWebhookServerAsync(
                        serversCoroutinesContext,
                        monoWebhookUrl,
                        events::dispatch,
                    )
                logger.info("Mono webhook listener started.")
                logger.info("Setup completed. Listening...\n")

                telegramServerJob.join()
                monoWebhookServer.join()
            }
        } catch (e: Throwable) {
            logger.error(e) {
                "Unhandled exception"
            }
        }
    }
}

fun main(args: Array<String>) {
    YnabMono().main(args)
}
