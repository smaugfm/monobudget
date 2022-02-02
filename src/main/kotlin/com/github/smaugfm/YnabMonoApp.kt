package com.github.smaugfm

import com.github.smaugfm.apis.MonoApis
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.models.settings.Mappings
import com.github.smaugfm.workflows.ProcessError
import com.github.smaugfm.workflows.ProcessTelegramCallbackWorkflow
import com.github.smaugfm.workflows.ProcessWebhookWorkflow
import com.github.smaugfm.workflows.SendTelegramMessageWorkflow
import io.ktor.util.error
import kotlinx.coroutines.flow.collect
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI

private val logger = KotlinLogging.logger {}

class YnabMonoApp : KoinComponent {
    val telegramApi by inject<TelegramApi>()
    val monoApis by inject<MonoApis>()
    val mappings by inject<Mappings>()

    val createTransaction by inject<ProcessWebhookWorkflow>()
    val sendMessage by inject<SendTelegramMessageWorkflow>()
    val processCallbackQuery by inject<ProcessTelegramCallbackWorkflow>()
    val processError by inject<ProcessError>()

    suspend fun run(setWebhook: Boolean, monoWebhookUrl: URI, webhookPort: Int) {
        if (setWebhook) {
            logger.info("Setting up mono webhooks.")
            if (!monoApis.apis.all { it.setWebHook(monoWebhookUrl, webhookPort) })
                return
        } else {
            logger.info("Skipping mono webhook setup.")
        }

        monoApis.listenWebhooks(
            monoWebhookUrl,
            webhookPort,
        ).collect {
            try {
                val newTransaction = createTransaction(it) ?: return@collect
                sendMessage(it, newTransaction)
            } catch (e: Throwable) {
                logger.error(e)
                processError()
            }
        }

        telegramApi.listenForCallbacks().collect {
            processCallbackQuery(it)
        }
    }
}
