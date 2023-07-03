package io.github.smaugfm.monobudget

import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.CommonModule
import io.github.smaugfm.monobudget.common.model.BudgetBackend
import io.github.smaugfm.monobudget.common.model.BudgetBackend.Lunchmoney
import io.github.smaugfm.monobudget.common.model.BudgetBackend.YNAB
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.statement.StatementService
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyModule
import io.github.smaugfm.monobudget.mono.MonoModule
import io.github.smaugfm.monobudget.mono.MonoWebhookListener
import io.github.smaugfm.monobudget.ynab.YnabModule
import io.github.smaugfm.monobudget.ynab.model.YnabSaveTransaction
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ksp.generated.module
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
        setupKoin(settings, setWebhook, monoWebhookUrl, webhookPort)

        when (budgetBackend) {
            is Lunchmoney -> Application<LunchmoneyTransaction, LunchmoneyInsertTransaction>()
            is YNAB -> Application<YnabTransactionDetail, YnabSaveTransaction>()
        }.run()
    }
}

private fun CoroutineScope.setupKoin(
    settings: Settings,
    setWebhook: Boolean,
    monoWebhookUrl: URI,
    webhookPort: Int
) {
    startKoin {
        printLogger(Level.ERROR)

        modules(runtimeModule(settings, this@setupKoin))
        modules(
            MonoModule().module + module {
                single {
                    MonoWebhookListener(
                        setWebhook,
                        monoWebhookUrl,
                        webhookPort,
                        get(),
                        get()
                    )
                } bind StatementService::class
            }
        )
        modules(CommonModule().module)
        modules(
            when (settings.budgetBackend) {
                is Lunchmoney -> lunchmoneyModule(settings.budgetBackend)
                is YNAB -> ynabModule()
            }
        )
    }
}

private fun runtimeModule(settings: Settings, coroutineScope: CoroutineScope) = module {
    when (settings.budgetBackend) {
        is Lunchmoney ->
            single { settings.budgetBackend }

        is YNAB -> single { settings.budgetBackend }
    } bind BudgetBackend::class

    single { settings.mcc }
    single { settings.bot }
    single { settings.accounts }
    settings.transfer.forEach { s ->
        single { s }
    }
    single { coroutineScope }
}

private fun lunchmoneyModule(budgetBackend: Lunchmoney) = module {
    single { LunchmoneyApi(budgetBackend.token) }
} + LunchmoneyModule().module

private fun ynabModule() = listOf(YnabModule().module)
