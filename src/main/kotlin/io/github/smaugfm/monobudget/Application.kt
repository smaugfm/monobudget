package io.github.smaugfm.monobudget

import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.kotlin.retry.RetryConfig
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.smaugfm.monobudget.common.account.TransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.statement.StatementItemChecker
import io.github.smaugfm.monobudget.common.statement.StatementItemListener
import io.github.smaugfm.monobudget.common.statement.StatementService
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.injectAll
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import io.ktor.util.logging.error
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.onEach
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Duration
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

@OptIn(FlowPreview::class)
class Application<TTransaction, TNewTransaction> :
    KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val statementServices by injectAll<StatementService>()
    private val startupVerifiers by injectAll<ApplicationStartupVerifier>()
    private val transactionCreator by inject<TransactionFactory<TTransaction, TNewTransaction>>()
    private val messageFormatter by inject<TransactionMessageFormatter<TTransaction>>()
    private val transferDetector by inject<TransferBetweenAccountsDetector<TTransaction>>()
    private val telegramCallbackHandler by inject<TelegramCallbackHandler<TTransaction>>()
    private val errorHandler by inject<TelegramErrorUnknownErrorHandler>()
    private val telegramMessageSender by inject<TelegramMessageSender>()
    private val statementChecker by inject<StatementItemChecker>()
    private val statementListeners by injectAll<StatementItemListener>()

    @Suppress("MagicNumber")
    private val processStatementRetry = Retry.of(
        "processStatement",
        RetryConfig {
            maxAttempts(3)
            failAfterMaxAttempts(true)
            intervalFunction(
                IntervalFunction.ofExponentialBackoff(
                    Duration.ofSeconds(10),
                    2.0
                )
            )
        }
    )

    suspend fun run() {
        runStartupChecks()

        if (statementServices.any { !it.prepare() }) {
            return
        }

        val telegramJob = telegramApi.start(telegramCallbackHandler::handle)
        statementServices.asFlow()
            .flatMapMerge { it.statements() }
            .filter(statementChecker::check)
            .onEach { item -> statementListeners.forEach { it.onNewStatementItem(item) } }
            .onEach(::process)
            .collect()
        log.info { "Started application" }
        telegramJob.join()
    }

    private suspend fun process(statement: StatementItem) {
        try {
            processStatementRetry.executeSuspendFunction {
                try {
                    processStatement(statement)
                } catch (e: Throwable) {
                    log.error(e)
                    errorHandler.onRetry(statement)
                    throw e
                }
            }
        } catch (e: Throwable) {
            log.error(e)
            errorHandler.onUnknownError()
        }
    }

    private suspend fun processStatement(statement: StatementItem) {
        val maybeTransfer =
            transferDetector.checkTransfer(statement)

        val transaction = transactionCreator.create(maybeTransfer)
        val message = messageFormatter.format(
            statement,
            transaction
        )

        telegramMessageSender.send(statement.accountId, message)
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
