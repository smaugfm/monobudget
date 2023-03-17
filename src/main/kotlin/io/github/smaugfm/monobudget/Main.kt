package io.github.smaugfm.monobudget

import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.BudgetBackend
import io.github.smaugfm.monobudget.models.settings.Settings
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.service.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.service.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.service.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.service.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.service.transaction.CategorySuggestingService
import io.github.smaugfm.monobudget.service.transaction.PayeeSuggestingService
import io.github.smaugfm.monobudget.service.ynab.MonoStatementToYnabTransactionTransformer
import io.github.smaugfm.monobudget.service.ynab.YnabTransactionCreator
import io.github.smaugfm.monobudget.service.ynab.YnabTransactionTelegramMessageFormatter
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

private const val DEFAULT_HTTP_PORT = 80

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
                    single(qualifier(BudgetBackend.YNAB)) {
                        MonoTransferBetweenAccountsDetector<YnabTransactionDetail>()
                    }
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
