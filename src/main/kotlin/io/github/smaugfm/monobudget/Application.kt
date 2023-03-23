package io.github.smaugfm.monobudget

import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.components.callback.TelegramCallbackHandler
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.components.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.components.mono.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.components.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.components.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.components.transaction.BudgetTransactionCreator
import io.github.smaugfm.monobudget.components.verification.ApplicationStartupVerifier
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

class Application<TTransaction, TNewTransaction>(private val monoSettings: Settings.MultipleMonoSettings) :
    KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val webhooksListener by inject<MonoWebhookListenerServer>()

    private val transactionCreator by inject<BudgetTransactionCreator<TTransaction, TNewTransaction>>()
    private val messageFormatter by inject<TransactionMessageFormatter<TTransaction>>()
    private val monoTransferDetector by inject<MonoTransferBetweenAccountsDetector<TTransaction>>()

    private val telegramCallbackHandler by inject<TelegramCallbackHandler<TTransaction>>()
    private val processError by inject<TelegramErrorUnknownErrorHandler>()
    private val telegramMessageSender by inject<TelegramMessageSender>()
    private val webhookResponseDuplicateFilter by inject<DuplicateWebhooksFilter>()

    suspend fun run(setWebhook: Boolean, monoWebhookUrl: URI, webhookPort: Int) {
        runStartupChecks()

        if (setWebhook) {
            log.info { "Setting up mono webhooks." }
            if (!webhooksListener.setupWebhook(monoWebhookUrl, webhookPort)) {
                log.error { "Error settings up webhooks. Exiting application..." }
                exitProcess(1)
            }
        } else {
            log.info { "Skipping mono webhook setup." }
        }

        val webhookJob = webhooksListener.start(
            monoWebhookUrl,
            webhookPort
        ) handler@{ responseData ->
            try {
                if (!monoSettings.monoAccountsIds.contains(responseData.account)) {
                    log.info {
                        "Skipping transaction from Monobank " +
                            "accountId=${responseData.account} because this account is not configured."
                    }
                    return@handler
                }

                if (webhookResponseDuplicateFilter.checkIsDuplicate(responseData)) {
                    return@handler
                }

                val maybeTransfer = monoTransferDetector.checkTransfer(responseData)

                val transaction = transactionCreator.create(maybeTransfer)
                val message = messageFormatter.format(
                    responseData,
                    transaction
                )

                telegramMessageSender.send(responseData.account, message)
            } catch (e: Throwable) {
                log.error(e)
                processError()
            }
        }
        val telegramJob = telegramApi.start(telegramCallbackHandler::handle)
        log.info { "Listening for Monobank webhooks and Telegram callbacks..." }
        webhookJob.join()
        telegramJob.join()
    }

    private suspend fun runStartupChecks() {
        try {
            getKoin().getAll<ApplicationStartupVerifier>().forEach { it.verify() }
        } catch (e: Throwable) {
            log.error(e) { "Failed to start application. Exiting..." }
            exitProcess(1)
        }
    }
}
