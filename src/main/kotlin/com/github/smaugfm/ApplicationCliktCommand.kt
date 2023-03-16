package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.smaugfm.server.MonoWebhookListenerServer
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.api.YnabApi
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.DEFAULT_HTTP_PORT
import com.github.smaugfm.workflow.CreateTransaction
import com.github.smaugfm.workflow.HandleTelegramCallback
import com.github.smaugfm.workflow.ProcessError
import com.github.smaugfm.workflow.RetryWithRateLimit
import com.github.smaugfm.workflow.SendHTMLMessageToTelegram
import com.github.smaugfm.workflow.SendTransactionCreatedMessage
import com.github.smaugfm.workflow.TransformStatementToYnabTransaction
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class ApplicationCliktCommand : CliktCommand() {
    private val setWebhook by option().flag(default = false)
    private val monoWebhookUrl by option().convert { URI(it) }.required()
    private val webhookPort by option().int().default(DEFAULT_HTTP_PORT)
    private val settings by option().convert {
        Settings.load(Paths.get(it))
    }.required()

    override fun run() {
        logger.debug(
            "Parsed args:\n\t" +
                "${this::settings.name}: $settings\n\t" +
                "${this::monoWebhookUrl.name}: $monoWebhookUrl\n\t" +
                "${this::webhookPort.name}: $webhookPort\n\t" +
                "${this::setWebhook.name}: $setWebhook",
        )

        runBlocking {
            startKoin {
                modules(
                    module {
                        printLogger(org.koin.core.logger.Level.ERROR)

                        single { settings }
                        single { settings.mappings }
                        single { YnabApi(get()) }
                        single { TelegramApi(this@runBlocking, get()) }
                        single { MonoWebhookListenerServer(this@runBlocking, get()) }
                        single {
                            TransformStatementToYnabTransaction(
                                this@runBlocking, get(), get()
                            )
                        }
                        single { CreateTransaction(get(), get(), get()) }
                        single { SendHTMLMessageToTelegram(get(), get()) }
                        single { RetryWithRateLimit(get()) }
                        single { SendTransactionCreatedMessage(get(), get()) }
                        single { ProcessError(get(), get()) }
                        single { HandleTelegramCallback(get(), get(), get(), get()) }
                    }
                )
            }

            Application().run(setWebhook, monoWebhookUrl, webhookPort)
        }
    }
}

fun main(args: Array<String>) {
    logger.info("Passed args: ${args.joinToString(", ") { "\"$it\"" }}")

    ApplicationCliktCommand().main(args)
}
