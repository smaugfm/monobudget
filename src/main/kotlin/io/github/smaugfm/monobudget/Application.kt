package io.github.smaugfm.monobudget

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.smaugfm.monobudget.common.BaseApplication
import io.github.smaugfm.monobudget.common.notify.TelegramApi
import io.github.smaugfm.monobudget.common.notify.TelegramCallbackHandler
import io.github.smaugfm.monobudget.common.startup.ApplicationStartupVerifier
import io.github.smaugfm.monobudget.common.statement.StatementSource
import io.github.smaugfm.monobudget.common.util.injectAll
import org.koin.core.component.inject
import kotlin.system.exitProcess

private val log = KotlinLogging.logger {}

class Application<TTransaction, TNewTransaction> :
    BaseApplication<TTransaction, TNewTransaction>() {
    override val statementSources: List<StatementSource> by injectAll<StatementSource>()
    private val telegramApi by inject<TelegramApi>()
    private val startupVerifiers by injectAll<ApplicationStartupVerifier>()
    private val telegramCallbackHandler by inject<TelegramCallbackHandler<TTransaction>>()

    override suspend fun beforeStart() {
        try {
            startupVerifiers.forEach { it.verify() }
        } catch (e: Throwable) {
            log.error(e) { "Failed to start application. Exiting..." }
            exitProcess(1)
        }
    }

    override suspend fun afterSourcesPrepare() {
        telegramApi.start(telegramCallbackHandler::handle)
    }
}
