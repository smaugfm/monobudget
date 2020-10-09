package com.github.smaugfm.wrappers

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.smaugfm.events.ExternalEvent
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class TelegramApi(telegram: Bot) {
    companion object {
        suspend fun startTelegramServerAsync(
            context: CoroutineContext,
            telegramToken: String,
            dispatch: suspend (ExternalEvent) -> Unit,
        ): Pair<Deferred<Unit>, TelegramApi> =
            withContext(context) {
                val telegram = bot {
                    token = telegramToken
                    dispatch {
                        callbackQuery("testButton") { _, update ->
                            runBlocking {
                                TODO(dispatch.toString())
                            }
                        }
                    }
                }

                Pair(
                    async { telegram.startPolling() },
                    TelegramApi(telegram)
                )
            }
    }
}