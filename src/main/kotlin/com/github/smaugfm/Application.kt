package com.github.smaugfm

import com.github.smaugfm.server.MonoWebhookListenerServer
import com.github.smaugfm.api.TelegramApi
import com.github.smaugfm.workflow.CreateTransaction
import com.github.smaugfm.workflow.HandleCallback
import com.github.smaugfm.workflow.ProcessError
import com.github.smaugfm.workflow.RetryWithRateLimit
import com.github.smaugfm.workflow.SendTransactionCreatedMessage
import io.ktor.util.logging.error
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI

private val logger = KotlinLogging.logger {}

class Application : KoinComponent {
    private val telegramApi by inject<TelegramApi>()
    private val monoApis by inject<MonoWebhookListenerServer>()

    private val createTransaction by inject<CreateTransaction>()
    private val retryWithRateLimit by inject<RetryWithRateLimit>()
    private val sendTransactionCreatedMessage by inject<SendTransactionCreatedMessage>()
    private val handleCallback by inject<HandleCallback>()
    private val processError by inject<ProcessError>()

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
        ) { responseData ->
            try {
                retryWithRateLimit(responseData.account) retry@{
                    val newTransaction = createTransaction(responseData) ?: return@retry
                    sendTransactionCreatedMessage(responseData, newTransaction)
                }
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
