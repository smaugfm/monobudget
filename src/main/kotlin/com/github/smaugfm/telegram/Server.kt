package com.github.smaugfm.telegram

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.smaugfm.processing.Event
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

suspend fun startTelegramServer(
    context: CoroutineContext,
    telegramToken: String,
    handler: suspend (Event.TelegramAction) -> Unit,
): Pair<Job, Bot> =
    withContext(context) {
        val telegram = bot {
            token = telegramToken
            dispatch {
                callbackQuery("testButton") { bot, update ->
                    runBlocking {
                        handler(TODO())
                    }
                }
            }
        }

        Pair(
            launch { telegram.startPolling() },
            telegram
        )
    }
