package io.github.smaugfm.monobudget.import

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.lunchmoney.model.LunchmoneyInsertTransaction
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobudget.common.BaseApplication
import io.github.smaugfm.monobudget.common.account.TransferCache
import io.github.smaugfm.monobudget.common.model.settings.Settings
import io.github.smaugfm.monobudget.common.notify.StatementItemNotificationSender
import io.github.smaugfm.monobudget.common.retry.InMemoryStatementRetryRepository
import io.github.smaugfm.monobudget.common.transaction.NewTransactionFactory
import io.github.smaugfm.monobudget.lunchmoney.LunchmoneyNewTransactionFactory
import io.github.smaugfm.monobudget.mono.MonoWebhookSettings
import io.github.smaugfm.monobudget.setupKoinModules
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val log = KotlinLogging.logger {}

class ImporterApplication(private val source: ImporterStatementSource) :
    BaseApplication<LunchmoneyTransaction, LunchmoneyInsertTransaction>() {
    override val statementSources = listOf(source)

    override suspend fun run() {
        super.run()
        log.info { "Import finished" }
        exitProcess(0)
    }

    companion object {
        suspend fun main(coroutineScope: CoroutineScope) {
            val settings =
                Settings.load(
                    Paths.get(System.getenv()["SETTINGS_FILE"] ?: "settings.yml"),
                )
            val importConfig =
                ImporterConfig.load(
                    Paths.get(System.getenv()["IMPORT_CONFIG_FILE"] ?: "import-config.yml"),
                )
            val noteSuffix = " monobudget-import-${Clock.System.now()}"
            log.info {
                "Importing transactions from files: \n${
                    importConfig.getImports().map { it.transactionsFileContent }.joinToString("\n")
                }\nUsing 'note' field suffix: '${noteSuffix.trim()}'"
            }

            startKoin {
                setupKoinModules(
                    coroutineScope,
                    InMemoryStatementRetryRepository(),
                    settings,
                    MonoWebhookSettings(false, URI.create("none://none"), 0),
                )
                modules(
                    module {
                        single<NewTransactionFactory<LunchmoneyInsertTransaction>> {
                            LunchmoneyNewTransactionFactory(noteSuffix)
                        }
                        single<TransferCache<LunchmoneyTransaction>> {
                            ImporterTransferCache(7.seconds)
                        }
                        single<StatementItemNotificationSender> { ImporterNotificationSender }
                    },
                )
            }.koin

            ImporterApplication(ImporterStatementSource(importConfig.getImports())).run()
        }
    }
}
