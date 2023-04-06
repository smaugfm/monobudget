package io.github.smaugfm.monobudget

import injectAll
import io.github.smaugfm.monobudget.common.statement.StatementService
import io.github.smaugfm.monobudget.common.telegram.TelegramApi
import io.github.smaugfm.monobudget.common.telegram.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.common.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.common.transaction.TransactionFactory
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.verify.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.mono.MonoWebhookResponseChecker
import io.github.smaugfm.monobudget.mono.TransferBetweenAccountsDetector
import io.ktor.util.logging.error
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
    private val processError by inject<TelegramErrorUnknownErrorHandler>()
    private val telegramMessageSender by inject<TelegramMessageSender>()
    private val webhookResponseChecker by inject<MonoWebhookResponseChecker>()

    suspend fun run() {
        runStartupChecks()

        if (statementServices.any { !it.prepare() }) {
            return
        }

        val telegramJob = telegramApi.start(telegramCallbackHandler::handle)
        statementServices.asFlow()
            .flatMapMerge { it.statements() }
            .collect handler@{ responseData ->

                try {
                    if (!webhookResponseChecker.check(responseData)) {
                        return@handler
                    }

                    val maybeTransfer =
                        transferDetector.checkTransfer(responseData)

                    val transaction = transactionCreator.create(maybeTransfer)
                    val message = messageFormatter.format(
                        responseData,
                        transaction
                    )

                    telegramMessageSender.send(responseData.accountId, message)
                } catch (e: Throwable) {
                    log.error(e)
                    processError()
                }
            }
        log.info { "Listening for Monobank webhooks and Telegram callbacks..." }
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
