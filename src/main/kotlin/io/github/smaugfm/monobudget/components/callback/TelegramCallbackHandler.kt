package io.github.smaugfm.monobudget.components.callback

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import com.elbekd.bot.types.InlineKeyboardButton
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.ParseMode
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.components.formatter.TransactionMessageFormatter
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import mu.KotlinLogging
import kotlin.reflect.KClass

private val log = KotlinLogging.logger {}

sealed class TelegramCallbackHandler<TTransaction>(
    protected val telegram: TelegramApi,
    private val formatter: TransactionMessageFormatter<TTransaction>,
    private val telegramChatIds: List<Long>
) {
    suspend fun handle(callbackQuery: CallbackQuery) {
        if (callbackQuery.from.id !in telegramChatIds) {
            log.warn { "Received Telegram callbackQuery from unknown chatId: ${callbackQuery.from.id}" }
            return
        }

        val (callbackQueryId, transactionUpdateType, message) = extractFromCallbackQuery(callbackQuery) ?: return

        val updatedTransaction = updateTransaction(transactionUpdateType).also {
            telegram.answerCallbackQuery(callbackQueryId)
        }

        val updatedText = updateHTMLStatementMessage(updatedTransaction, message)
        val updatedMarkup = updateMarkupKeyboard(updatedTransaction, transactionUpdateType, message.replyMarkup!!)

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
    protected abstract suspend fun updateHTMLStatementMessage(
        updatedTransaction: TTransaction,
        oldMessage: Message
    ): String

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
        updatedTransaction: TTransaction,
        transactionUpdateType: TransactionUpdateType,
        oldKeyboard: InlineKeyboardMarkup
    ): InlineKeyboardMarkup =
        basedOnOldKeyboard(
            oldKeyboard,
            formatter.getReplyKeyboardPressedButtons(updatedTransaction, transactionUpdateType)
        )

    private fun basedOnOldKeyboard(
        oldKeyboard: InlineKeyboardMarkup,
        pressed: Set<KClass<out TransactionUpdateType>>
    ): InlineKeyboardMarkup {
        return oldKeyboard.inlineKeyboard.map { outer ->
            outer.map { oldButton ->
                val cls =
                    TransactionUpdateType::class.sealedSubclasses.find { it.simpleName == oldButton.callbackData }!!
                InlineKeyboardButton(
                    TransactionUpdateType.buttonText(
                        cls,
                        cls in pressed,
                    ),
                    callbackData = TransactionUpdateType.serialize(cls)
                )
            }
        }.let(::InlineKeyboardMarkup)
    }

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
