package io.github.smaugfm.monobudget

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.lifecycle.StatementItemProcessor
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingEventDelivery
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.statement.StatementService
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.util.injectAll
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class Application<TTransaction, TNewTransaction> :
    KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val statementServices by injectAll<StatementService>()
    private val startupVerifiers by injectAll<ApplicationStartupVerifier>()
    private val telegramCallbackHandler by inject<TelegramCallbackHandler<TTransaction>>()
    private val statementEvents by inject<StatementProcessingEventDelivery>()

    suspend fun run() {
        runStartupChecks()

        if (statementServices.any { !it.prepare() }) {
            return
        }

        val telegramJob = telegramApi.start(telegramCallbackHandler::handle)
        statementServices.asFlow()
            .flatMapMerge { it.statements() }
            .filter(statementEvents::onNewStatement)
            .map(::StatementProcessingScopeComponent)
            .onEach {
                with(it) {
                    try {
                        scope.get<StatementItemProcessor<TTransaction, TNewTransaction>>()
                            .process()
                        statementEvents.onStatementEnd(ctx)
                    } catch (e: Throwable) {
                        statementEvents.onStatementError(ctx, e)
                    } finally {
                        scope.close()
                    }
                }
            }
            .collect()
        log.info { "Started application" }
        telegramJob.join()
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
