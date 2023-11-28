package io.github.smaugfm.monobudget

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.exception.BudgetBackendException
import io.github.smaugfm.monobudget.common.startup.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementEvents
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.statement.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.util.injectAll
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

class Application<TTransaction, TNewTransaction> :
    KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val statementSources by injectAll<StatementSource>()
    private val startupVerifiers by injectAll<ApplicationStartupVerifier>()
    private val telegramCallbackHandler by inject<TelegramCallbackHandler<TTransaction>>()
    private val statementEvents by inject<StatementEvents>()

    suspend fun run() {
        runStartupChecks()

        statementSources.forEach { it.prepare() }

        telegramApi.start(telegramCallbackHandler::handle)
        log.info { "Started application" }

        statementSources.asFlow()
            .flatMapMerge { it.statements() }
            .filter(statementEvents::onNewStatement)
            .map(::StatementProcessingScopeComponent)
            .onEach {
                with(it) {
                    try {
                        scope.get<StatementItemProcessor<TTransaction, TNewTransaction>>()
                            .process()
                        statementEvents.onStatementEnd(ctx)
                    } catch (e: BudgetBackendException) {
                        statementEvents.onStatementRetry(ctx, e)
                    } catch (e: Throwable) {
                        statementEvents.onStatementError(ctx, e)
                    } finally {
                        scope.close()
                    }
                }
            }
            .collect()
    }

    private suspend fun runStartupChecks() {
        try {
            startupVerifiers.forEach { it.verify() }
        } catch (e: Throwable) {
            log.error(e) { "Failed to start application. Exiting..." }
            exitProcess(1)
        }
    }
}
