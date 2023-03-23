package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.ParseMode
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.model.TransactionUpdateType.Companion.buttonWord
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter.Companion.formatInlineKeyboard
import mu.KotlinLogging
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

sealed class TelegramCallbackHandler<TTransaction>(
    protected val telegram: TelegramApi,
    private val telegramChatIds: List<Long>
) {
    suspend fun handle(callbackQuery: CallbackQuery) {
        if (callbackQuery.from.id !in telegramChatIds) {
            log.warn { "Received Telegram callbackQuery from unknown chatId: ${callbackQuery.from.id}" }
            return
        }

        val (callbackQueryId, type, message) = extractFromCallbackQuery(callbackQuery) ?: return

        val updatedTransaction = updateTransaction(type).also {
            telegram.answerCallbackQuery(callbackQueryId)
        }

        val updatedText = updateHTMLStatementMessage(updatedTransaction, message)
        val updatedMarkup = updateMarkupKeyboard(type, message.replyMarkup!!)

        if (stripHTMLTagsFromMessage(updatedText) != message.text ||
            updatedMarkup != message.replyMarkup
        ) {
            with(message) {
                telegram.editMessage(
                    ChatId.IntegerId(chat.id),
                    messageId,
                    updatedText,
                    ParseMode.Html,
                    updatedMarkup
                )
            }
        }
    }

    protected abstract suspend fun updateTransaction(type: TransactionUpdateType): TTransaction
    protected abstract suspend fun updateHTMLStatementMessage(updatedTransaction: TTransaction, oldMessage: Message): String

    private suspend fun extractFromCallbackQuery(callbackQuery: CallbackQuery): CallbackQueryData? {
        val callbackQueryId = callbackQuery.id
        val data = callbackQuery.data.takeUnless { it.isNullOrBlank() }
            ?: log.warn { "Received Telegram callbackQuery with empty data.\n$callbackQuery" }
                .let { return null }
        val message =
            callbackQuery.message ?: log.warn { "Received Telegram callbackQuery with empty message" }
                .let { return null }

        val type = TransactionUpdateType.deserialize(data, message)
            ?: return null.also {
                telegram.answerCallbackQuery(
                    callbackQueryId,
                    TelegramApi.UNKNOWN_ERROR_MSG
                )
            }


        return CallbackQueryData(callbackQueryId, type, message)
    }

    private fun updateMarkupKeyboard(
        type: TransactionUpdateType,
        oldKeyboard: InlineKeyboardMarkup
    ): InlineKeyboardMarkup =
        formatInlineKeyboard(pressedButtons(oldKeyboard) + type::class)

    private fun pressedButtons(oldKeyboard: InlineKeyboardMarkup): Set<KClass<out TransactionUpdateType>> =
        oldKeyboard
            .inlineKeyboard
            .flatten()
            .filter { it.text.contains(TransactionUpdateType.pressedChar) }
            .mapNotNull { button ->
                TransactionUpdateType::class.sealedSubclasses.find {
                    button.text.contains(it.buttonWord())
                }
            }.toSet()

    private fun stripHTMLTagsFromMessage(messageText: String): String {
        val replaceHtml = Regex("<.*?>")
        return replaceHtml.replace(messageText, "")
    }

    private data class CallbackQueryData(
        val callbackQueryId: String,
        val type: TransactionUpdateType,
        val message: Message
    )
}
