package com.github.smaugfm

import com.github.smaugfm.apis.MonoApis
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.workflows.CreateTransaction
import com.github.smaugfm.workflows.HandleCallback
import com.github.smaugfm.workflows.ProcessError
import com.github.smaugfm.workflows.SendMessage
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
    val sendMessage by inject<SendMessage>()
    val handleCallback by inject<HandleCallback>()
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
        ) handler@{
            try {
                val newTransaction = createTransaction(it) ?: return@handler
                sendMessage(it, newTransaction)
            } catch (e: Throwable) {
                logger.error(e)
                processError()
            }
        }
        val telegramJob = telegramApi.listenForCallbacks {
            handleCallback(it)
        }
        logger.info { "Listening for Monobank webhooks and Telegram callbacks..." }
        webhookJob.join()
        telegramJob.join()
    }
}
