package com.github.smaugfm

import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.api.YnabApi
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.server.MonoWebhookListenerServer
import com.github.smaugfm.util.DEFAULT_HTTP_PORT
import com.github.smaugfm.service.ynab.CreateYnabTransaction
import com.github.smaugfm.service.telegram.TelegramCallbackHandler
import com.github.smaugfm.service.telegram.TelegramErrorUnknownErrorHandler
import com.github.smaugfm.service.ynab.RetryWithYnabRateLimit
import com.github.smaugfm.service.telegram.TelegramHTMLMessageSender
import com.github.smaugfm.service.ynab.SendYnabTransactionCreatedMessage
import com.github.smaugfm.service.ynab.TransformStatementToYnabTransaction
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

fun main() {
    val env = System.getenv()
    val setWebhook = env["SET_WEBHOOK"]?.toBoolean() ?: false
    val monoWebhookUrl = URI(env["MONO_WEBHOOK_URL"]!!)
    val webhookPort = env["WEBHOOK_PORT"]?.toInt() ?: DEFAULT_HTTP_PORT
    val settings = Settings.load(Paths.get(env["SETTINGS"]!!))
    logger.debug(
        "Startup options:\n\t" +
            "setWebhook: $setWebhook\n\t",
        "monoWebhookUrl: $monoWebhookUrl\n\t" +
            "webhookPort: $webhookPort\n\t" +
            "settings: $settings"
    )
    runBlocking {
        startKoin {
            modules(
                module {
                    printLogger(Level.ERROR)

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
                    single { CreateYnabTransaction(get(), get(), get()) }
                    single { TelegramHTMLMessageSender(get(), get()) }
                    single { RetryWithYnabRateLimit(get()) }
                    single { SendYnabTransactionCreatedMessage(get(), get()) }
                    single { TelegramErrorUnknownErrorHandler(get(), get()) }
                    single { TelegramCallbackHandler(get(), get(), get(), get()) }
                }
            )
        }

        Application().run(setWebhook, monoWebhookUrl, webhookPort)
    }
}
