package com.github.smaugfm

import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.api.YnabApi
import com.github.smaugfm.models.BudgetBackend
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.server.MonoWebhookListenerServer
import com.github.smaugfm.service.MonoTransferBetweenAccountsDetector
import com.github.smaugfm.service.mono.DuplicateWebhooksFilter
import com.github.smaugfm.service.mono.MonoAccountsService
import com.github.smaugfm.service.telegram.TelegramCallbackHandler
import com.github.smaugfm.service.telegram.TelegramErrorUnknownErrorHandler
import com.github.smaugfm.service.telegram.TelegramMessageSender
import com.github.smaugfm.service.transaction.CategorySuggestingService
import com.github.smaugfm.service.transaction.PayeeSuggestingService
import com.github.smaugfm.service.ynab.MonoStatementToYnabTransactionTransformer
import com.github.smaugfm.service.ynab.YnabTransactionCreator
import com.github.smaugfm.service.ynab.YnabTransactionTelegramMessageFormatter
import com.github.smaugfm.util.DEFAULT_HTTP_PORT
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.qualifier
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
                    single { MonoAccountsService(settings) }
                    single { CategorySuggestingService(settings) }
                    single { PayeeSuggestingService() }
                    single { YnabApi(get()) }
                    single { TelegramApi(this@runBlocking, get()) }
                    single { MonoWebhookListenerServer(this@runBlocking, get()) }
                    single { DuplicateWebhooksFilter(get()) }
                    single(qualifier(BudgetBackend.YNAB)) { MonoTransferBetweenAccountsDetector<YnabTransactionDetail>() }
                    single {
                        MonoStatementToYnabTransactionTransformer(this@runBlocking, get(), get(), get(), get())
                    }
                    single { YnabTransactionCreator(get(), get(), get()) }
                    single { TelegramMessageSender(get(), get()) }
                    single { YnabTransactionTelegramMessageFormatter(get()) }

                    val telegramChaIds = settings.mappings.monoAccToTelegram.values.toSet()
                    single { TelegramErrorUnknownErrorHandler(telegramChaIds, get()) }
                    single {
                        TelegramCallbackHandler(
                            get(),
                            get(),
                            telegramChaIds,
                            settings.mappings.unknownCategoryId,
                            settings.mappings.unknownCategoryId
                        )
                    }
                }
            )
        }

        Application().run(setWebhook, monoWebhookUrl, webhookPort)
    }
}
