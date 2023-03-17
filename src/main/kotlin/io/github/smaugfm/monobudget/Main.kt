package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.models.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.models.settings.Settings
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.service.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.service.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.service.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.service.telegram.ynab.YnabTelegramCallbackHandler
import io.github.smaugfm.monobudget.service.transaction.CategorySuggestingService
import io.github.smaugfm.monobudget.service.transaction.PayeeSuggestingService
import io.github.smaugfm.monobudget.service.ynab.MonoStatementToYnabTransactionTransformer
import io.github.smaugfm.monobudget.service.ynab.YnabTransactionCreator
import io.github.smaugfm.monobudget.service.ynab.YnabTransactionTelegramMessageFormatter
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

private const val DEFAULT_HTTP_PORT = 80

@Suppress("LongMethod")
fun main() {
    val env = System.getenv()
    val setWebhook = env["SET_WEBHOOK"]?.toBoolean() ?: false
    val monoWebhookUrl = URI(env["MONO_WEBHOOK_URL"]!!)
    val webhookPort = env["WEBHOOK_PORT"]?.toInt() ?: DEFAULT_HTTP_PORT
    val settings = Settings.load(Paths.get(env["SETTINGS"] ?: "settings.json"))
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
                    val telegramChaIds = settings.mappings.monoAccToTelegram.values.toSet()

                    when (settings.budgetBackend) {
                        is Lunchmoney -> {
                            single { LunchmoneyApi(settings.budgetBackend.token) }
                        }

                        is YNAB -> {
                            single { YnabApi(settings.budgetBackend) }
                            single { YnabTransactionCreator(get(), get(), get()) }
                            single { YnabTransactionTelegramMessageFormatter(get()) }
                            single {
                                YnabTelegramCallbackHandler(
                                    get(),
                                    get(),
                                    telegramChaIds,
                                    settings.mappings.unknownCategoryId,
                                    settings.mappings.unknownCategoryId
                                )
                            }
                            single {
                                MonoStatementToYnabTransactionTransformer(
                                    this@runBlocking,
                                    get(),
                                    get(),
                                    get(),
                                    get()
                                )
                            }
                        }
                    }
                    single { MonoWebhookListenerServer(this@runBlocking, get()) }
                    single { TelegramApi(this@runBlocking, get()) }
                    single { TelegramMessageSender(get(), get()) }
                    single { TelegramErrorUnknownErrorHandler(telegramChaIds, get()) }
                    single { DuplicateWebhooksFilter(get()) }
                    single { MonoTransferBetweenAccountsDetector() }
                    single { MonoAccountsService(settings) }
                    single { CategorySuggestingService(settings) }
                    single { PayeeSuggestingService() }
                }
            )
        }

        Application().run(setWebhook, monoWebhookUrl, webhookPort)
    }
}
