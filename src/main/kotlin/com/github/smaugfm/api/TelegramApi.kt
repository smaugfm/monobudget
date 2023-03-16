package com.github.smaugfm.api

import com.elbekd.bot.Bot
import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.pp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class TelegramApi(
    private val scope: CoroutineScope,
    private val settings: Settings
) {
    private val bot: Bot =
        Bot.createPolling(settings.telegramBotUsername, settings.telegramBotToken)

    suspend fun sendMessage(
        chatId: ChatId,
        text: String,
        parseMode: ParseMode? = null,
        replyMarkup: ReplyKeyboard? = null
    ) {
        bot.sendMessage(
            chatId,
            text,
            null,
            parseMode,
            null,
            null,
            true,
            null,
            null,
            null,
            replyMarkup
        ).also {
            logger.debug { "Sending message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${replyMarkup?.pp()}" }
        }
    }

    @Suppress("LongParameterList")
    suspend fun editMessage(
        chatId: ChatId,
        messageId: Long,
        text: String,
        parseMode: ParseMode,
        replyMarkup: InlineKeyboardMarkup
    ) {
        bot.editMessageText(
            chatId,
            messageId,
            null,
            text,
            parseMode,
            null,
            false,
            replyMarkup
        ).also {
            logger.debug { "Updating message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${replyMarkup.pp()}" }
        }
    }

    suspend fun answerCallbackQuery(id: String, text: String? = null) {
        bot.answerCallbackQuery(id, text = text)
    }

    fun start(
        callbackHandler: suspend (CallbackQuery) -> Unit,
    ): Job {
        bot.onCallbackQuery {
            logger.debug { "Received callbackQuery.\n\t$it" }
            callbackHandler(it)
        }
        return scope.launch(context = Dispatchers.IO) {
            bot.start()
        }
    }

    companion object {
        const val UNKNOWN_ERROR_MSG = "Виникла невідома помилка"
    }
}
