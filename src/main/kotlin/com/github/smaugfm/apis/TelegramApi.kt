package com.github.smaugfm.apis

import com.elbekD.bot.Bot
import com.github.smaugfm.events.Event
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class TelegramApi private constructor(val bot: Bot) {
    fun startServer(
        context: CoroutineContext,
        dispatch: suspend (Event) -> Unit,
    ): Job {

        bot.onCallbackQuery {
            dispatch(TODO())
        }

        return GlobalScope.launch(context) { bot.start() }
    }

    companion object {
        fun create(
            botUsername: String,
            botToken: String,
        ): TelegramApi {
            val telegram = Bot.createPolling(botUsername, botToken)
            return TelegramApi(telegram)
        }
    }
}