package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.models.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.models.Settings
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.service.callback.YnabTelegramCallbackHandler
import io.github.smaugfm.monobudget.service.formatter.LunchmoneyTransactionMessageFormatter
import io.github.smaugfm.monobudget.service.formatter.YnabTransactionMessageFormatter
import io.github.smaugfm.monobudget.service.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.mono.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.service.statement.MonoStatementToYnabTransactionTransformer
import io.github.smaugfm.monobudget.service.suggesting.LunchmoneyCategorySuggestingServiceImpl
import io.github.smaugfm.monobudget.service.suggesting.StringSimilarityPayeeSuggestingService
import io.github.smaugfm.monobudget.service.suggesting.YnabCategorySuggestingService
import io.github.smaugfm.monobudget.service.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.service.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.service.transaction.LunchmoneyTransactionCreator
import io.github.smaugfm.monobudget.service.transaction.YnabTransactionCreator
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
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
    val settings = Settings.load(Paths.get(env["SETTINGS"] ?: "settings.json"))
    val budgetBackend = settings.budgetBackend
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
                    val telegramChaIds = settings.mono.telegramChatIds

                    when (budgetBackend) {
                        is Lunchmoney -> {
                            single { LunchmoneyApi(budgetBackend.token) }
                            single { MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>() }
                            single { LunchmoneyTransactionCreator(get(), get()) }
                            single { LunchmoneyTransactionMessageFormatter }
                            single { LunchmoneyCategorySuggestingServiceImpl(get(), settings.mcc, get()) }
                        }

                        is YNAB -> {
                            single { YnabCategorySuggestingService(get(), settings.mcc, get()) }
                            single { MonoTransferBetweenAccountsDetector<YnabTransactionDetail>() }
                            single { YnabApi(budgetBackend) }
                            single { YnabTransactionCreator(get(), get(), get()) }
                            single { YnabTransactionMessageFormatter(get()) }
                            single {
                                YnabTelegramCallbackHandler(
                                    get(),
                                    get(),
                                    telegramChaIds
                                )
                            }
                            single {
                                MonoStatementToYnabTransactionTransformer(
                                    get(),
                                    get(),
                                    get(),
                                    get(),
                                    get()
                                )
                            }
                        }
                    }
                    single { PeriodicFetcherFactory(get()) }
                    single { MonoWebhookListenerServer(this@runBlocking, settings.mono.apis) }
                    single { TelegramApi(this@runBlocking, settings.bot) }
                    single { TelegramMessageSender(get(), get()) }
                    single { TelegramErrorUnknownErrorHandler(telegramChaIds, get()) }
                    single { DuplicateWebhooksFilter(get()) }
                    single { MonoAccountsService(get(), settings.mono) }
                    single { StringSimilarityPayeeSuggestingService() }
                }
            )
        }

        when (budgetBackend) {
            is Lunchmoney -> Application<LunchmoneyTransaction>()
            is YNAB -> Application<YnabTransactionDetail>()
        }.run(setWebhook, monoWebhookUrl, webhookPort)
    }
}
