package com.github.smaugfm

import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.models.BudgetBackend
import com.github.smaugfm.models.ynab.YnabTransactionDetail
import com.github.smaugfm.server.MonoWebhookListenerServer
import com.github.smaugfm.service.MonoTransferBetweenAccountsDetector
import com.github.smaugfm.service.mono.DuplicateWebhooksFilter
import com.github.smaugfm.service.telegram.TelegramCallbackHandler
import com.github.smaugfm.service.telegram.TelegramErrorUnknownErrorHandler
import com.github.smaugfm.service.telegram.TelegramMessageSender
import com.github.smaugfm.service.ynab.YnabTransactionCreator
import com.github.smaugfm.service.ynab.YnabTransactionTelegramMessageFormatter
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.qualifier
import java.net.URI

private val logger = KotlinLogging.logger {}

class Application : KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val monoApis by inject<MonoWebhookListenerServer>()

    private val ynabTransactionCreator by inject<YnabTransactionCreator>()
    private val ynabNewTransactionMessageFormatter by inject<YnabTransactionTelegramMessageFormatter>()
    private val handleCallback by inject<TelegramCallbackHandler>()
    private val processError by inject<TelegramErrorUnknownErrorHandler>()
    private val telegramMessageSender by inject<TelegramMessageSender>()
    private val webhookResponseDuplicateFilter by inject<DuplicateWebhooksFilter>()
    private val monoTransferDetector by inject<MonoTransferBetweenAccountsDetector<YnabTransactionDetail>>(
        qualifier(
            BudgetBackend.YNAB
        )
    )

    suspend fun run(setWebhook: Boolean, monoWebhookUrl: URI, webhookPort: Int) {
        if (setWebhook) {
            logger.info { "Setting up mono webhooks." }
            if (!monoApis.apis.all { it.setWebHook(monoWebhookUrl, webhookPort) })
                return
        } else {
            logger.info { "Skipping mono webhook setup." }
        }

        val webhookJob = monoApis.start(
            monoWebhookUrl,
            webhookPort,
        ) handler@{ responseData ->
            try {
                if (webhookResponseDuplicateFilter.checkIsDuplicate(responseData))
                    return@handler

                val maybeTransfer = monoTransferDetector.checkTransfer(responseData)

                val newYnabTransaction =
                    ynabTransactionCreator.create(maybeTransfer)

                val (monoId, msg, markup) = ynabNewTransactionMessageFormatter.format(
                    responseData,
                    newYnabTransaction
                ) ?: return@handler

                telegramMessageSender.send(monoId, msg, markup)
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
