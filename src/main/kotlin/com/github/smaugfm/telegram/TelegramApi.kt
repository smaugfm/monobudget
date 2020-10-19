package com.github.smaugfm.telegram

import com.elbekD.bot.Bot
import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.events.Event
import com.github.smaugfm.util.getLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import java.net.URI
import kotlin.coroutines.CoroutineContext

class TelegramApi(
    botUsername: String,
    botToken: String,
    val allowedChatIds: Set<Int>,
    webhookUrl: URI? = null,
) {
    private val bot: Bot =
        if (webhookUrl != null)
            Bot.createWebhook(botUsername, botToken)
        else
            Bot.createPolling(botUsername, botToken)
    private val logger = getLogger()

    suspend fun sendMessage(
        chatId: Any,
        text: String,
        parseMode: String? = null,
        disableWebPagePreview: Boolean? = null,
        disableNotification: Boolean? = null,
        replyTo: Int? = null,
        markup: ReplyKeyboard? = null,
    ) {
        bot.sendMessage(
            chatId,
            text,
            parseMode,
            disableWebPagePreview,
            disableNotification,
            replyTo,
            markup
        ).asDeferred().await()
    }

    suspend fun answerCallbackQuery(id: String, text: String?) {
        bot.answerCallbackQuery(id, text = text).asDeferred().await()
    }

    fun startServer(
        context: CoroutineContext,
        dispatch: suspend (Event) -> Unit,
    ): Job {
        bot.onCallbackQuery {
            val chatId = it.from.id
            if (chatId !in allowedChatIds)
                return@onCallbackQuery

            it.data?.let { data ->
                dispatch(Event.Telegram.CallbackQueryReceived(it.id, data))
            } ?: logger.severe("Received callback query without callback_data.\n$it")
        }

        return GlobalScope.launch(context) { bot.start() }
    }
}
