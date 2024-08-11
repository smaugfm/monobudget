package io.github.smaugfm.monobudget.import

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.account.BankAccountService
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingContext
import io.github.smaugfm.monobudget.mono.MonoApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.decodeFromString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.StringQualifier
import java.io.File

private val log = KotlinLogging.logger {}

class ImporterStatementSource(private val configs: List<ImporterAccountConfig>) :
    StatementSource, KoinComponent {
    lateinit var statementItems: List<StatementItem>

    private val bankAccountsService: BankAccountService by inject()
    private val csv =
        Csv {
            hasHeaderRecord = false
            nullString = "—"
        }

    override suspend fun prepare() {
        statementItems =
            configs.map { config ->
                val accountId =
                    getKoin().get<MonoApi>(StringQualifier(config.accountAlias))
                        .accountId
                val accountCurrency = bankAccountsService.getAccountCurrency(accountId)!!
                val csvItems =
                    csv.decodeFromString<List<CsvMonoItem>>(
                        File(config.transactionsFileContent).readText()
                            .substringAfter("\n"),
                    )

                log.info { "Found ${csvItems.size} transactions for '${config.accountAlias}' account" }
                csvItems.map { it.toStatementItem(accountId, accountCurrency) }
            }.flatten()
                .sortedBy { it.time }
        log.info { "Importing total of ${statementItems.size} transactions" }
    }

    override suspend fun statements(): Flow<StatementProcessingContext> {
        return statementItems.map(::StatementProcessingContext).asFlow()
    }
}
