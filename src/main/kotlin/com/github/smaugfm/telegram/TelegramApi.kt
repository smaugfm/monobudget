package com.github.smaugfm.telegram

import com.elbekD.bot.Bot
import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEventDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger {}

class TelegramApi(
    botUsername: String,
    botToken: String,
    val allowedChatIds: Set<Int>,
) {
    private val bot: Bot =
        Bot.createPolling(botUsername, botToken)

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
        ).asDeferred().also {
            logger.info("Sending message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: $markup")
        }.await()
    }

    suspend fun answerCallbackQuery(id: String, text: String? = null) {
        bot.answerCallbackQuery(id, text = text).asDeferred().await()
    }

    fun startServer(
        context: CoroutineContext,
        dispatcher: IEventDispatcher,
    ): Job {
        bot.onCallbackQuery {
            logger.info("Received callbackQuery.\n\t$it")
            val chatId = it.from.id
            if (chatId !in allowedChatIds)
                return@onCallbackQuery

            it.data?.let { data ->
                dispatcher(Event.Telegram.CallbackQueryReceived(it.id, data, it.message?.text!!))
            } ?: logger.error("Received callback query without callback_data.\n$it")
        }

        return GlobalScope.launch(context) { bot.start() }
    }
}
