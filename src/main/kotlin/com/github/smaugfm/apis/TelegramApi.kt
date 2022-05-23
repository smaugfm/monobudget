package com.github.smaugfm.apis

import com.elbekD.bot.Bot
import com.elbekD.bot.http.await
import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.models.settings.Settings
import com.github.smaugfm.util.pp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.net.URL

private val logger = KotlinLogging.logger {}

class TelegramApi(
    private val scope: CoroutineScope,
    private val settings: Settings
) {
    private val bot: Bot =
        Bot.createPolling(settings.telegramBotUsername, settings.telegramBotToken)

    @Suppress("LongParameterList")
    suspend fun sendMessage(
        chatId: Any,
        text: String,
        parseMode: String? = null,
        disableWebPagePreview: Boolean? = null,
        disableNotification: Boolean? = null,
        replyTo: Long? = null,
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
            logger.debug { "Sending message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${markup?.pp()}" }
        }.await()
    }

    @Suppress("LongParameterList")
    suspend fun editMessage(
        chatId: Any? = null,
        messageId: Long? = null,
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
            logger.debug { "Updating message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${markup?.pp()}" }
        }.await()
    }

    suspend fun answerCallbackQuery(id: String, text: String? = null) {
        bot.answerCallbackQuery(id, text = text).asDeferred().await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(
        callbackHandler: suspend (CallbackQuery) -> Unit,
        csvFileHandler: suspend (Long, String) -> Unit,
    ): Job {
        bot.onCallbackQuery {
            logger.debug { "Received callbackQuery.\n\t$it" }
            callbackHandler(it)
        }
        bot.onMessage { msg ->
            val doc = msg.document
            if (doc != null && doc.file_name?.endsWith(".csv") == true) {
                val file = bot.getFile(doc.file_id).await()
                val url = URL(
                    "https://api.telegram.org/" +
                        "file/${settings.telegramBotToken}/${file.file_path}"
                )
                csvFileHandler(
                    msg.chat.id,
                    withContext(Dispatchers.IO) {
                        url.openStream().readAllBytes().toString(Charsets.UTF_8)
                    }
                )
            }
        }
        return scope.launch(context = Dispatchers.IO) {
            bot.start()
        }
    }

    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
    }
}
