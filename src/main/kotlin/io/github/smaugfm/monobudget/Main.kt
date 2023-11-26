package io.github.smaugfm.monobudget

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.RetryConfig
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.CommonModule
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.common.model.settings.MonoAccountSettings
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.retry.JacksonFileStatementRetryRepository
import io.github.smaugfm.monobudget.common.retry.StatementRetryRepository
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyModule
import io.github.smaugfm.monobudget.mono.MonoApi
import io.github.smaugfm.monobudget.mono.MonoModule
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.github.smaugfm.monobudget.ynab.YnabModule
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.*
import java.net.URI
import java.nio.file.Paths
import java.time.Duration

private val log = KotlinLogging.logger {}

private const val DEFAULT_HTTP_PORT = 80

fun main() {
    val env = System.getenv()
    val setWebhook = env["SET_WEBHOOK"]?.toBoolean() ?: false
    val monoWebhookUrl = URI(env["MONO_WEBHOOK_URL"]!!)
    val webhookPort = env["WEBHOOK_PORT"]?.toInt() ?: DEFAULT_HTTP_PORT
    val settings = Settings.load(Paths.get(env["SETTINGS_FILE"] ?: "settings.yml"))
    val jsonRetryRepository = JacksonFileStatementRetryRepository(
        Paths.get(env["RETRIES_FILE"] ?: "retries.json")
    )
    val budgetBackend = settings.budgetBackend
    val webhookSettings = MonoWebhookSettings(setWebhook, monoWebhookUrl, webhookPort)

    log.debug { "Startup options: " }
    log.debug { "\twebhookSettings: $webhookSettings" }

    runBlocking {
        startKoin {
            setupKoinModules(this@runBlocking, jsonRetryRepository, settings, webhookSettings)
        }

        when (budgetBackend) {
            is Lunchmoney -> Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>()
            is YNAB -> Application<YnabTransactionDetail, YnabSaveTransaction>()
        }.run()
    }
}

fun KoinApplication.setupKoinModules(
    coroutineScope: CoroutineScope,
    retryRepository: StatementRetryRepository,
    settings: Settings,
    webhookSettings: MonoWebhookSettings,
) {
    printLogger(Level.ERROR)
    modules(runtimeModule(coroutineScope, retryRepository, settings, webhookSettings))
    modules(MonoModule().module)
    modules(CommonModule().module)
    modules(
        when (settings.budgetBackend) {
            is Lunchmoney -> lunchmoneyModule(settings.budgetBackend)
            is YNAB -> ynabModule()
        },
    )
}

private fun runtimeModule(
    coroutineScope: CoroutineScope,
    retryRepository: StatementRetryRepository,
    settings: Settings,
    webhookSettings: MonoWebhookSettings,
) = module {
    when (settings.budgetBackend) {
        is Lunchmoney -> single { settings.budgetBackend }

        is YNAB -> single { settings.budgetBackend }
    } bind BudgetBackend::class

    single { webhookSettings }
    single { retryRepository }
    single { settings.mcc }
    single { settings.bot }
    single { settings.accounts }
    single { settings.retry }
    single { apiRetry() }
    settings.accounts.settings.filterIsInstance<MonoAccountSettings>()
        .forEach { s -> single { MonoApi(s.token, s.accountId) } }
    settings.transfer.forEach { s ->
        single(qualifier = StringQualifier(s.descriptionRegex.pattern)) { s }
    }
    single { coroutineScope }
}

@Suppress("MagicNumber")
private fun apiRetry(): Retry =
    Retry.of(
        "apiRetry",
        RetryConfig {
            maxAttempts(3)
            failAfterMaxAttempts(true)
            intervalFunction(
                IntervalFunction.ofExponentialBackoff(
                    Duration.ofSeconds(1),
                    2.0,
                ),
            )
        },
    )

private fun lunchmoneyModule(budgetBackend: Lunchmoney) =
    module {
        single {
            LunchmoneyApi(
                budgetBackend.token,
                requestTransformer = RetryOperator.of(get()),
            )
        }
    } + LunchmoneyModule().module

private fun ynabModule() = listOf(YnabModule().module)
