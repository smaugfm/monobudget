package com.github.smaugfm

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import com.github.smaugfm.apis.MonoApis
import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.DEFAULT_HTTP_PORT
import com.github.smaugfm.workflows.ProcessError
import com.github.smaugfm.workflows.ProcessTelegramCallbackWorkflow
import com.github.smaugfm.workflows.ProcessWebhookWorkflow
import com.github.smaugfm.workflows.SendTelegramMessageWorkflow
import com.github.smaugfm.workflows.util.MonoWebhookResponseToYnabTransactionConverter
import com.github.smaugfm.workflows.util.UniqueWebhookResponses
import com.github.smaugfm.workflows.util.YnabTransferPayeeIdsCache
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.net.URI
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

class YnabMonoCommand : CliktCommand() {
    private val setWebhook by option().flag(default = false)
    private val monoWebhookUrl by option().convert { URI(it) }.required()
    private val webhookPort by option().int().default(DEFAULT_HTTP_PORT)
    private val settings by option().convert {
        Settings.load(Paths.get(it))
    }.required()

    override fun run() {
        logger.info(
            "Parsed args:\n\t" +
                "${this::settings.name}: $settings\n\t" +
                "${this::monoWebhookUrl.name}: $monoWebhookUrl\n\t" +
                "${this::webhookPort.name}: $webhookPort\n\t" +
                "${this::setWebhook.name}: $setWebhook",
        )

        runBlocking {
            startKoin {
                modules(
                    module {
                        single { settings }
                        single { settings.mappings }
                        single { YnabApi(get()) }
                        single { TelegramApi(this@runBlocking, get()) }
                        single { MonoApis(this@runBlocking, get()) }
                        single {
                            MonoWebhookResponseToYnabTransactionConverter(
                                this@runBlocking,
                                get()
                            ) { get<YnabApi>().getPayees(false) }
                        }
                        single { UniqueWebhookResponses() }
                        single { YnabTransferPayeeIdsCache(get()) }

                        single { ProcessWebhookWorkflow(get(), get(), get(), get(), get()) }
                        single { SendTelegramMessageWorkflow(get(), get()) }
                        single { ProcessError(get(), get()) }
                        single { ProcessTelegramCallbackWorkflow(get(), get(), get()) }
                    }
                )
            }

            YnabMonoApp().run(setWebhook, monoWebhookUrl, webhookPort)
        }
    }
}

fun main(args: Array<String>) {
    logger.info("Passed args: ${args.joinToString(", ") { "\"$it\"" }}")

    YnabMonoCommand().main(args)
}
