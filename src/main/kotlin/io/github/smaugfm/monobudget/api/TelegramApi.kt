package io.github.smaugfm.monobudget.api

import com.elbekd.bot.Bot
import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.ParseMode
import com.elbekd.bot.types.ReplyKeyboard
import io.github.smaugfm.monobudget.model.Settings
import io.github.smaugfm.monobudget.util.pp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val log = KotlinLogging.logger {}

class TelegramApi : KoinComponent {
    private val scope: CoroutineScope by inject()
    private val botSettings: Settings.TelegramBotSettings by inject()

    private val bot: Bot =
        Bot.createPolling(botSettings.token, botSettings.username)

    suspend fun sendMessage(
        chatId: ChatId,
        text: String,
        parseMode: ParseMode? = null,
        disableNotification: Boolean? = null,
        replyMarkup: ReplyKeyboard? = null
    ) {
        bot.sendMessage(
            chatId = chatId,
            text = text,
            messageThreadId = null,
            parseMode = parseMode,
            entities = null,
            disableWebPagePreview = true,
            disableNotification = disableNotification,
            protectContent = null,
            replyToMessageId = null,
            allowSendingWithoutReply = null,
            replyMarkup = replyMarkup
        ).also {
            log.debug { "Sending message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${replyMarkup?.pp()}" }
        }
    }

    suspend fun editKeyboard(chatId: ChatId, messageId: Long, replyMarkup: InlineKeyboardMarkup) {
        bot.editMessageReplyMarkup(
            chatId,
            messageId,
            null,
            replyMarkup
        )
    }

    suspend fun editMessage(
        chatId: ChatId,
        messageId: Long,
        text: String,
        parseMode: ParseMode?,
        replyMarkup: InlineKeyboardMarkup
    ) {
        bot.editMessageText(
            chatId,
            messageId,
            null,
            text,
            parseMode,
            null,
            true,
            replyMarkup
        ).also {
            log.debug { "Updating message. \n\tTo: $chatId\n\ttext: $text\n\tkeyboard: ${replyMarkup.pp()}" }
        }
    }

    suspend fun answerCallbackQuery(id: String, text: String? = null) {
        bot.answerCallbackQuery(id, text = text)
    }

    fun start(callbackHandler: suspend (CallbackQuery) -> Unit): Job {
        bot.onCallbackQuery {
            log.debug { "Received callbackQuery.\n\t$it" }
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
