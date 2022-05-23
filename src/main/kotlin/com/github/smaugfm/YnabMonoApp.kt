package com.github.smaugfm

import com.github.smaugfm.apis.MonoApis
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.workflows.CreateTransaction
import com.github.smaugfm.workflows.HandleCallback
import com.github.smaugfm.workflows.HandleCsv
import com.github.smaugfm.workflows.ProcessError
import com.github.smaugfm.workflows.RetryWithRateLimit
import com.github.smaugfm.workflows.SendTransactionCreatedMessage
import io.ktor.util.error
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI

private val logger = KotlinLogging.logger {}

class YnabMonoApp : KoinComponent {
    val telegramApi by inject<TelegramApi>()
    val monoApis by inject<MonoApis>()

    val createTransaction by inject<CreateTransaction>()
    val retryWithRateLimit by inject<RetryWithRateLimit>()
    val sendTransactionCreatedMessage by inject<SendTransactionCreatedMessage>()
    val handleCallback by inject<HandleCallback>()
    val handleCsv by inject<HandleCsv>()
    val processError by inject<ProcessError>()

    suspend fun run(setWebhook: Boolean, monoWebhookUrl: URI, webhookPort: Int) {
        if (setWebhook) {
            logger.info { "Setting up mono webhooks." }
            if (!monoApis.apis.all { it.setWebHook(monoWebhookUrl, webhookPort) })
                return
        } else {
            logger.info { "Skipping mono webhook setup." }
        }

        val webhookJob = monoApis.listenWebhooks(
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
        val telegramJob = telegramApi.start(
            handleCallback::invoke,
        ) { chatId, file ->
            handleCsv(chatId, file)
        }
        logger.info { "Listening for Monobank webhooks and Telegram callbacks..." }
        webhookJob.join()
        telegramJob.join()
    }
}
