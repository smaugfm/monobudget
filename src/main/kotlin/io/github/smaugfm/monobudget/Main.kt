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
import io.github.smaugfm.monobudget.components.suggestion.LunchmoneyCategorySuggestionService
import io.github.smaugfm.monobudget.components.suggestion.StringSimilarityPayeeSuggestionService
import io.github.smaugfm.monobudget.components.suggestion.YnabCategorySuggestionService
import io.github.smaugfm.monobudget.components.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.components.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.components.transaction.creator.BudgetTransactionCreator
import io.github.smaugfm.monobudget.components.transaction.creator.LunchmoneyTransactionCreator
import io.github.smaugfm.monobudget.components.transaction.creator.YnabTransactionCreator
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
            is Lunchmoney -> Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>(settings.mono)
            is YNAB -> Application<YnabTransactionDetail, YnabSaveTransaction>(settings.mono)
        }.run(setWebhook, monoWebhookUrl, webhookPort)
    }
}

private fun CoroutineScope.setupKoin(settings: Settings) {
    startKoin {
        modules(
            module {
                printLogger(Level.ERROR)
                single { settings.mcc }
                single { settings.bot }
                single { settings.mono }
                single { this@setupKoin }

                single<ApplicationStartupVerifier> { MonoSettingsVerifier() }
                single<ApplicationStartupVerifier> { BudgetSettingsVerifier() }
                single { PeriodicFetcherFactory() }
                single { MonoWebhookListenerServer() }
                single { TelegramApi() }
                single { TelegramMessageSender() }
                single { TelegramErrorUnknownErrorHandler() }
                single { DuplicateWebhooksFilter() }
                single { MonoAccountsService() }
                single { StringSimilarityPayeeSuggestionService() }

                when (settings.budgetBackend) {
                    is Lunchmoney -> setupForLunchmoney(settings.budgetBackend)

                    is YNAB -> setupForYnab(settings.budgetBackend)
                }
            }
        )
    }
}

private fun Module.setupForLunchmoney(budgetBackend: Lunchmoney) {
    single { budgetBackend } bind BudgetBackend::class

    single { LunchmoneyApi(budgetBackend.token) }
    single { MonoTransferBetweenAccountsDetector<LunchmoneyTransaction>() }
    single<NewTransactionFactory<LunchmoneyInsertTransaction>> {
        LunchmoneyNewTransactionFactory()
    }
    single<BudgetTransactionCreator<LunchmoneyTransaction, LunchmoneyInsertTransaction>> {
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

private fun Module.setupForYnab(budgetBackend: YNAB) {
    single { budgetBackend } bind BudgetBackend::class

    single<NewTransactionFactory<YnabSaveTransaction>> {
        YnabNewTransactionFactory()
    }
    single<CategorySuggestionService>(createdAtStart = true) {
        YnabCategorySuggestionService()
    }
    single { MonoTransferBetweenAccountsDetector<YnabTransactionDetail>() }
    single { YnabApi() }
    single<BudgetTransactionCreator<YnabTransactionDetail, YnabSaveTransaction>> {
        YnabTransactionCreator()
    }
    single<TransactionMessageFormatter<YnabTransactionDetail>> {
        YnabTransactionMessageFormatter()
    }
    single<TelegramCallbackHandler<YnabTransactionDetail>> {
        YnabTelegramCallbackHandler()
    }
    single(createdAtStart = true) {
        YnabNewTransactionFactory()
    }
    single<ApplicationStartupVerifier> {
        YnabCurrencyVerifier()
    }
}
