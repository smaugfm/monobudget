package com.github.smaugfm.telegram

import com.elbekD.bot.Bot
import com.github.smaugfm.events.Event
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class TelegramApi private constructor(val bot: Bot) {
    private val logger = Logger.getLogger(TelegramApi::class.qualifiedName.toString())

    fun startServer(
        context: CoroutineContext,
        dispatch: suspend (Event) -> Unit,
    ): Job {
        bot.onCallbackQuery {
            it.data?.let { data ->
                dispatch(Event.Telegram.CallbackQueryReceived(data))
            } ?: logger.severe("Received callback query without callback_data.\n$it")
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