package com.github.smaugfm.telegram.handlers

import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import mu.KotlinLogging
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class MessagesHandler : Handler() {
    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@MessagesHandler::handleRestart)
        registerUnit(this@MessagesHandler::handleStop)
    }

    private fun handleRestart(event: Event.Telegram.RestartCommandReceived) {
        logger.info("Exiting with exit code 1 due to restart command.")
        exitProcess(1)
    }

    private fun handleStop(event: Event.Telegram.StopCommandReceived) {
        logger.info("Exiting with exit code 0 due to stop command.")
        exitProcess(0)
    }
}
