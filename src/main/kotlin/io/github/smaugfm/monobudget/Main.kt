package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.components.callback.LunchmoneyTelegramCallbackHandler
import io.github.smaugfm.monobudget.components.callback.TelegramCallbackHandler
import io.github.smaugfm.monobudget.components.callback.YnabTelegramCallbackHandler
import io.github.smaugfm.monobudget.components.formatter.LunchmoneyTransactionMessageFormatter
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.components.formatter.YnabTransactionMessageFormatter
import io.github.smaugfm.monobudget.components.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.components.suggestion.LunchmoneyCategorySuggestionServiceImpl
import io.github.smaugfm.monobudget.components.suggestion.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.components.suggestion.YnabCategorySuggestionService
import io.github.smaugfm.monobudget.components.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.components.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.components.transaction.BudgetTransactionCreator
import io.github.smaugfm.monobudget.components.transaction.LunchmoneyTransactionCreator
import io.github.smaugfm.monobudget.components.transaction.YnabTransactionCreator
import io.github.smaugfm.monobudget.components.transaction.factory.LunchmoneyNewTransactionFactory
import io.github.smaugfm.monobudget.components.transaction.factory.NewTransactionFactory
import io.github.smaugfm.monobudget.components.transaction.factory.YnabNewTransactionFactory
import io.github.smaugfm.monobudget.components.verification.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.components.verification.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.components.verification.MonoSettingsVerifier
import io.github.smaugfm.monobudget.components.verification.YnabCurrencyVerifier
import io.github.smaugfm.monobudget.model.BudgetBackend
import io.github.smaugfm.monobudget.model.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.model.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.util.PeriodicFetcherFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.Module
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

private const val DEFAULT_HTTP_PORT = 80

fun main() {
    val env = System.getenv()
    val setWebhook = env["SET_WEBHOOK"]?.toBoolean() ?: false
    val monoWebhookUrl = URI(env["MONO_WEBHOOK_URL"]!!)
    val webhookPort = env["WEBHOOK_PORT"]?.toInt() ?: DEFAULT_HTTP_PORT
    val settings = Settings.load(Paths.get(env["SETTINGS"] ?: "settings.yml"))
    val budgetBackend = settings.budgetBackend
    log.debug { "Startup options: " }
    log.debug { "\tsetWebhook: $setWebhook" }
    log.debug { "\tmonoWebhookUrl: $monoWebhookUrl" }
    log.debug { "\twebhookPort: $webhookPort" }
    runBlocking {
        setupKoin(settings, budgetBackend)

        when (budgetBackend) {
            is Lunchmoney -> Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>(settings.mono)
            is YNAB -> Application<YnabTransactionDetail, YnabSaveTransaction>(settings.mono)
        }.run(setWebhook, monoWebhookUrl, webhookPort)
    }
}

private fun CoroutineScope.setupKoin(settings: Settings, budgetBackend: BudgetBackend) {
    startKoin {
        modules(
            module {
                printLogger(Level.ERROR)
                val telegramChaIds = settings.mono.telegramChatIds

                single<ApplicationStartupVerifier> { MonoSettingsVerifier(settings.mono) }
                single<ApplicationStartupVerifier> { BudgetSettingsVerifier(budgetBackend, settings.mono) }
                single { PeriodicFetcherFactory(this@setupKoin) }
                single { MonoWebhookListenerServer(this@setupKoin, settings.mono.apis) }
                single { TelegramApi(this@setupKoin, settings.bot) }
                single { TelegramMessageSender(get(), get()) }
                single { TelegramErrorUnknownErrorHandler(telegramChaIds, get()) }
                single { DuplicateWebhooksFilter(get()) }
                single(createdAtStart = true) { MonoAccountsService(get(), settings.mono) }
                single { StringSimilarityPayeeSuggestionService() }

                when (budgetBackend) {
                    is Lunchmoney -> {
                        setupForLunchmoney(budgetBackend, settings, telegramChaIds)
                    }

                    is YNAB -> {
                        setupForYnab(budgetBackend, settings, telegramChaIds)
                    }
                }
            }
        )
    }
}

private fun Module.setupForLunchmoney(budgetBackend: Lunchmoney, settings: Settings, telegramChaIds: List<Long>) {
    single { LunchmoneyApi(budgetBackend.token) }
    single { MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>() }
    single<NewTransactionFactory<LunchmoneyInsertTransaction>> {
        LunchmoneyNewTransactionFactory(get(), get())
    }
    single<BudgetTransactionCreator<LunchmoneyTransaction, LunchmoneyInsertTransaction>> {
        LunchmoneyTransactionCreator(get(), budgetBackend.transferCategoryId.toLong(), get())
    }
    single<TransactionMessageFormatter<LunchmoneyTransaction>> {
        LunchmoneyTransactionMessageFormatter(get(), get())
    }
    single<CategorySuggestionService>(createdAtStart = true) {
        LunchmoneyCategorySuggestionServiceImpl(get(), settings.mcc, get())
    }
    single<TelegramCallbackHandler<LunchmoneyTransaction>> {
        LunchmoneyTelegramCallbackHandler(get(), get(), get(), get(), telegramChaIds)
    }
}

private fun Module.setupForYnab(budgetBackend: YNAB, settings: Settings, telegramChaIds: List<Long>) {
    single<NewTransactionFactory<YnabSaveTransaction>> {
        YnabNewTransactionFactory(get(), get(), get(), get(), get())
    }
    single<CategorySuggestionService>(createdAtStart = true) {
        YnabCategorySuggestionService(get(), settings.mcc, get())
    }
    single { MonoTransferBetweenAccountsDetector<YnabTransactionDetail>() }
    single { YnabApi(budgetBackend) }
    single<BudgetTransactionCreator<YnabTransactionDetail, YnabSaveTransaction>> {
        YnabTransactionCreator(get(), get(), get())
    }
    single<TransactionMessageFormatter<YnabTransactionDetail>> {
        YnabTransactionMessageFormatter(get())
    }
    single<TelegramCallbackHandler<YnabTransactionDetail>> {
        YnabTelegramCallbackHandler(get(), get(), get(), telegramChaIds)
    }
    single(createdAtStart = true) {
        YnabNewTransactionFactory(get(), get(), get(), get(), get())
    }
    single<ApplicationStartupVerifier> {
        YnabCurrencyVerifier(
            budgetBackend,
            settings.mono,
            get()
        )
    }
}
