package com.github.smaugfm.telegram

import com.elbekD.bot.Bot
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.util.pp
import kotlinx.coroutines.DelicateCoroutinesApi
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
            null,
            disableWebPagePreview,
            disableNotification,
            replyTo,
            null,
            markup
        ).asDeferred().also {
            logger.info("Sending message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${markup?.pp()}")
        }.await()
    }

    suspend fun editMessage(
        chatId: Any? = null,
        messageId: Int? = null,
        inlineMessageId: String? = null,
        text: String,
        parseMode: String? = null,
        disableWebPagePreview: Boolean? = null,
        markup: InlineKeyboardMarkup? = null,
    ) {
        bot.editMessageText(
            chatId,
            messageId,
            inlineMessageId,
            text,
            parseMode,
            null,
            disableWebPagePreview,
            markup
        ).asDeferred().also {
            logger.info("Updating message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${markup?.pp()}")
        }.await()
    }

    suspend fun answerCallbackQuery(id: String, text: String? = null) {
        bot.answerCallbackQuery(id, text = text).asDeferred().await()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startServer(
        context: CoroutineContext,
        dispatcher: IEventDispatcher,
    ): Job {
        bot.onCallbackQuery {
            logger.info("Received callbackQuery.\n\t$it")
            dispatcher(Event.Telegram.CallbackQueryReceived(it))
        }
        return GlobalScope.launch(context) { bot.start() }
    }
}
