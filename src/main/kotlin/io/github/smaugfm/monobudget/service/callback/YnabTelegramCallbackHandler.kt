package io.github.smaugfm.monobudget.service.callback

import com.elbekd.bot.model.ChatId
import com.elbekd.bot.types.CallbackQuery
import com.elbekd.bot.types.InlineKeyboardMarkup
import com.elbekd.bot.types.Message
import com.elbekd.bot.types.MessageEntity
import com.elbekd.bot.types.ParseMode
import io.github.smaugfm.monobudget.api.TelegramApi
import io.github.smaugfm.monobudget.api.YnabApi
import io.github.smaugfm.monobudget.models.YnabTransactionUpdateType
import io.github.smaugfm.monobudget.models.YnabTransactionUpdateType.Companion.buttonWord
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.service.formatter.YnabTransactionMessageFormatter.Companion.formatHTMLStatementMessage
import io.github.smaugfm.monobudget.service.formatter.YnabTransactionMessageFormatter.Companion.formatInlineKeyboard
import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class YnabTelegramCallbackHandler(
    private val telegram: TelegramApi,
    private val ynabApi: YnabApi,
    private val telegramChatIds: List<Long>
) {
    suspend operator fun invoke(callbackQuery: CallbackQuery) {
        if (callbackQuery.from.id !in telegramChatIds) {
            logger.warn { "Received Telegram callbackQuery from unknown chatId: ${callbackQuery.from.id}" }
            return
        }

        val (callbackQueryId, data, message) =
            extractFromCallbackQuery(callbackQuery) ?: return

        val type = YnabTransactionUpdateType.deserialize(data, message)
            ?: return Unit.also {
                telegram.answerCallbackQuery(
                    callbackQueryId,
                    TelegramApi.UNKNOWN_ERROR_MSG
                )
            }

        updateAndSendMessage(type, callbackQueryId, message)
    }

    private suspend fun updateAndSendMessage(
        type: YnabTransactionUpdateType,
        callbackQueryId: String,
        message: Message
    ) {
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

    private suspend fun updateTransaction(type: YnabTransactionUpdateType): YnabTransactionDetail {
        val transactionDetail = ynabApi.getTransaction(type.transactionId)
        val saveTransaction = transactionDetail.toSaveTransaction()

        val newTransaction = when (type) {
            is YnabTransactionUpdateType.Uncategorize ->
                saveTransaction.copy(categoryId = null, payeeName = null, payeeId = null)

            is YnabTransactionUpdateType.Unapprove ->
                saveTransaction.copy(approved = false)

            is YnabTransactionUpdateType.MakePayee -> saveTransaction.copy(payeeId = null, payeeName = type.payee)
        }

        return ynabApi.updateTransaction(transactionDetail.id, newTransaction)
    }

    @Suppress("MagicNumber")
    private fun updateHTMLStatementMessage(updatedTransaction: YnabTransactionDetail, oldMessage: Message): String {
        val oldText = oldMessage.text!!
        val oldTextLines = oldText.split("\n").filter { it.isNotBlank() }
        val description = oldMessage.entities.find { it.type == MessageEntity.Type.BOLD }
            ?.run { oldMessage.text!!.substring(offset, offset + length) }!!

        val mcc = oldTextLines[2].trim()
        val currencyText = oldTextLines[3].trim()
        val id = oldTextLines[6].trim()

        return formatHTMLStatementMessage(
            null,
            description,
            mcc,
            currencyText,
            updatedTransaction.categoryName ?: "",
            updatedTransaction.payeeName ?: "",
            id
        )
    }

    private fun extractFromCallbackQuery(callbackQuery: CallbackQuery): Triple<String, String, Message>? {
        val callbackQueryId = callbackQuery.id
        val data = callbackQuery.data.takeUnless { it.isNullOrBlank() }
            ?: logger.warn { "Received Telegram callbackQuery with empty data.\n$callbackQuery" }
                .let { return null }
        val message =
            callbackQuery.message ?: logger.warn { "Received Telegram callbackQuery with empty message" }
                .let { return null }

        return Triple(callbackQueryId, data, message)
    }

    private fun pressedButtons(oldKeyboard: InlineKeyboardMarkup): Set<KClass<out YnabTransactionUpdateType>> =
        oldKeyboard
            .inlineKeyboard
            .flatten()
            .filter { it.text.contains(YnabTransactionUpdateType.pressedChar) }
            .mapNotNull { button ->
                YnabTransactionUpdateType::class.sealedSubclasses.find {
                    button.text.contains(it.buttonWord())
                }
            }.toSet()

    private fun updateMarkupKeyboard(
        type: YnabTransactionUpdateType,
        oldKeyboard: InlineKeyboardMarkup
    ): InlineKeyboardMarkup = formatInlineKeyboard(pressedButtons(oldKeyboard) + type::class)

    companion object {
        internal fun stripHTMLTagsFromMessage(messageText: String): String {
            val replaceHtml = Regex("<.*?>")
            return replaceHtml.replace(messageText, "")
        }
    }
}
