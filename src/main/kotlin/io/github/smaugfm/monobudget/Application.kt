package io.github.smaugfm.monobudget

import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.server.MonoWebhookListenerServer
import io.github.smaugfm.monobudget.service.callback.YnabTelegramCallbackHandler
import io.github.smaugfm.monobudget.service.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.service.mono.DuplicateWebhooksFilter
import io.github.smaugfm.monobudget.service.mono.MonoTransferBetweenAccountsDetector
import io.github.smaugfm.monobudget.service.telegram.TelegramErrorUnknownErrorHandler
import io.github.smaugfm.monobudget.service.telegram.TelegramMessageSender
import io.github.smaugfm.monobudget.service.transaction.TransactionCreator
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class Application<TTransaction> : KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val webhooksListener by inject<MonoWebhookListenerServer>()

    private val transactionCreator by inject<TransactionCreator<TTransaction>>()
    private val messageFormatter by inject<TransactionMessageFormatter<TTransaction>>()
    private val monoTransferDetector by inject<MonoTransferBetweenAccountsDetector<TTransaction>>()

    private val handleCallback by inject<YnabTelegramCallbackHandler>()
    private val processError by inject<TelegramErrorUnknownErrorHandler>()
    private val telegramMessageSender by inject<TelegramMessageSender>()
    private val webhookResponseDuplicateFilter by inject<DuplicateWebhooksFilter>()

    suspend fun run(setWebhook: Boolean, monoWebhookUrl: URI, webhookPort: Int) {
        if (setWebhook) {
            logger.info { "Setting up mono webhooks." }
            if (!webhooksListener.setupWebhook(monoWebhookUrl, webhookPort)) {
                logger.error { "Error settings up webhooks. Exiting application..." }
                exitProcess(1)
            }
        } else {
            logger.info { "Skipping mono webhook setup." }
        }

        val webhookJob = webhooksListener.start(
            monoWebhookUrl,
            webhookPort
        ) handler@{ responseData ->
            try {
                if (webhookResponseDuplicateFilter.checkIsDuplicate(responseData)) {
                    return@handler
                }

                val maybeTransfer = monoTransferDetector.checkTransfer(responseData)

                val transaction = transactionCreator.create(maybeTransfer)
                val message = messageFormatter.format(
                    responseData,
                    transaction
                ) ?: return@handler

                telegramMessageSender.send(responseData.account, message)
            } catch (e: Throwable) {
                logger.error(e)
                processError()
            }
        }
        val telegramJob = telegramApi.start(handleCallback::invoke)
        logger.info { "Listening for Monobank webhooks and Telegram callbacks..." }
        webhookJob.join()
        telegramJob.join()
    }
}
