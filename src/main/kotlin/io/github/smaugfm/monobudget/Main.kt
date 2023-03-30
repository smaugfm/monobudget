package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.CategorySuggestionService
import io.github.smaugfm.monobudget.common.misc.PeriodicFetcherFactory
import io.github.smaugfm.monobudget.common.misc.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.common.model.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.common.model.Settings
import io.github.smaugfm.monobudget.common.model.ynab.YnabSaveTransaction
import io.github.smaugfm.monobudget.common.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.common.mono.MonoAccountsService
import io.github.smaugfm.monobudget.common.mono.MonoSettingsVerifier
import io.github.smaugfm.monobudget.common.mono.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.mono.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.telegram.TelegramWebhookResponseChecker
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.verify.BudgetSettingsVerifier
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyCategorySuggestionService
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyMonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyNewTransactionFactory
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyTelegramCallbackHandler
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyTransactionCreator
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyTransactionMessageFormatter
import io.github.smaugfm.monobudget.ynab.YnabApi
import io.github.smaugfm.monobudget.ynab.YnabCategorySuggestionService
import io.github.smaugfm.monobudget.ynab.YnabCurrencyVerifier
import io.github.smaugfm.monobudget.ynab.YnabMonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.ynab.YnabNewTransactionFactory
import io.github.smaugfm.monobudget.ynab.YnabTelegramCallbackHandler
import io.github.smaugfm.monobudget.ynab.YnabTransactionFactory
import io.github.smaugfm.monobudget.ynab.YnabTransactionMessageFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.bind
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
        setupKoin(settings)

        when (budgetBackend) {
            is Lunchmoney -> Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>()
            is YNAB -> Application<YnabTransactionDetail, YnabSaveTransaction>()
        }.run(setWebhook, monoWebhookUrl, webhookPort)
    }
}

private fun CoroutineScope.setupKoin(settings: io.github.smaugfm.monobudget.common.model.Settings) {
    startKoin {
        printLogger(Level.ERROR)

        modules(runtimeModule(settings, this@setupKoin))
        modules(commonModule())
        modules(
            when (settings.budgetBackend) {
                is Lunchmoney -> lunchmoneyModule(settings.budgetBackend)
                is YNAB -> ynabModule(settings.budgetBackend)
            }
        )
    }
}

private fun commonModule() = module {
    single<ApplicationStartupVerifier> { MonoSettingsVerifier() }
    single<ApplicationStartupVerifier> { BudgetSettingsVerifier() }
    single { PeriodicFetcherFactory() }
    single { MonoWebhookListenerServer() }
    single { TelegramApi() }
    single { TelegramMessageSender() }
    single { TelegramErrorUnknownErrorHandler() }
    single { TelegramWebhookResponseChecker() }
    single { MonoAccountsService() }
    single { StringSimilarityPayeeSuggestionService() }
}

private fun runtimeModule(
    settings: io.github.smaugfm.monobudget.common.model.Settings,
    coroutineScope: CoroutineScope
) = module {
    single { settings.mcc }
    single { settings.bot }
    single { settings.mono }
    single { coroutineScope }
}

private fun lunchmoneyModule(budgetBackend: Lunchmoney) = module {
    single { budgetBackend } bind io.github.smaugfm.monobudget.common.model.BudgetBackend::class

    single { LunchmoneyApi(budgetBackend.token) }
    single<MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>> {
        LunchmoneyMonoTransferBetweenAccountsDetector()
    }
    single<NewTransactionFactory<LunchmoneyInsertTransaction>> {
        LunchmoneyNewTransactionFactory()
    }
    single<TransactionFactory<LunchmoneyTransaction, LunchmoneyInsertTransaction>> {
        LunchmoneyTransactionCreator()
    }
    single<TransactionMessageFormatter<LunchmoneyTransaction>> {
        LunchmoneyTransactionMessageFormatter()
    }
    single<CategorySuggestionService>(createdAtStart = true) {
        LunchmoneyCategorySuggestionService()
    }
    single<TelegramCallbackHandler<LunchmoneyTransaction>> {
        LunchmoneyTelegramCallbackHandler()
    }
}

private fun ynabModule(budgetBackend: YNAB) = module {
    single { budgetBackend } bind io.github.smaugfm.monobudget.common.model.BudgetBackend::class

    single<NewTransactionFactory<YnabSaveTransaction>> {
        YnabNewTransactionFactory()
    }
    single<CategorySuggestionService>(createdAtStart = true) {
        YnabCategorySuggestionService()
    }
    single<MonoTransferBetweenAccountsDetector<YnabTransactionDetail>> {
        YnabMonoTransferBetweenAccountsDetector()
    }
    single { YnabApi() }
    single<TransactionFactory<YnabTransactionDetail, YnabSaveTransaction>> {
        YnabTransactionFactory()
    }
    single<TransactionMessageFormatter<YnabTransactionDetail>> {
        YnabTransactionMessageFormatter()
    }
    single<TelegramCallbackHandler<YnabTransactionDetail>> {
        YnabTelegramCallbackHandler()
    }
    single<ApplicationStartupVerifier> {
        YnabCurrencyVerifier()
    }
}
