package com.github.smaugfm.telegram.handlers

import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.MessageEntity
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.Handler
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TransactionUpdateType
import com.github.smaugfm.telegram.TransactionUpdateType.Companion.buttonWord
import com.github.smaugfm.ynab.YnabTransactionDetail
import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class CallbackQueryHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : Handler() {

    override fun HandlersBuilder.registerHandlerFunctions() {
        registerUnit(this@CallbackQueryHandler::handle)
    }

    suspend fun handle(
        dispatch: IEventDispatcher,
        event: Event.Telegram.CallbackQueryReceived,
    ) {
        val callbackQuery = event.callbackQuery
        if (callbackQuery.from.id !in mappings.getTelegramChatIds()) {
            logger.warn("Received Telegram callbackQuery from unknown chatId: ${callbackQuery.from.id}")
            return
        }

        val (callbackQueryId, data, message) =
            extractFromCallbackQuery(callbackQuery) ?: return

        val type = TransactionUpdateType.deserialize(data, message)
            ?: return Unit.also {
                telegram.answerCallbackQuery(
                    callbackQueryId,
                    TelegramHandlers.UNKNOWN_ERROR_MSG
                )
            }

        val updatedTransaction = dispatch(Event.Ynab.UpdateTransaction(type)).also {
            telegram.answerCallbackQuery(callbackQueryId)
        } ?: return

        val updatedText = updateHTMLStatementMessage(updatedTransaction, message)
        val updatedMarkup = updateMarkupKeyboard(type, message.reply_markup!!)

        if (stripHTMLTagsFromMessage(updatedText) != message.text ||
            updatedMarkup != message.reply_markup
        ) {
            with(message) {
                telegram.editMessage(
                    chat.id,
                    message_id,
                    text = updatedText,
                    parseMode = "HTML",
                    markup = updatedMarkup
                )
            }
        }
    }

    private fun updateHTMLStatementMessage(
        updatedTransaction: YnabTransactionDetail,
        oldMessage: Message,
    ): String {
        val oldText = oldMessage.text!!
        val oldTextLines = oldText.split("\n").filter { it.isNotBlank() }
        val description = oldMessage.entities?.find { it.type == MessageEntity.Types.BOLD.type }
            ?.run { oldMessage.text!!.substring(offset, offset + length) }!!

        val mcc = oldTextLines[2].trim()
        val currencyText = oldTextLines[3].trim()
        val id = oldTextLines[6].trim()

        return formatHTMLStatementMessage(
            description,
            mcc,
            currencyText,
            updatedTransaction.category_name ?: "",
            updatedTransaction.payee_name ?: "",
            id
        )
    }

    private fun extractFromCallbackQuery(callbackQuery: CallbackQuery): Triple<String, String, Message>? {
        val callbackQueryId = callbackQuery.id
        val data = callbackQuery.data.takeUnless { it.isNullOrBlank() }
            ?: logger.warn("Received Telegram callbackQuery with empty data.\n$callbackQuery")
                .let { return null }
        val message =
            callbackQuery.message ?: logger.warn("Received Telegram callbacQuery with empty message")
                .let { return null }

        return Triple(callbackQueryId, data, message)
    }

    private fun pressedButtons(oldKeyboard: InlineKeyboardMarkup): Set<KClass<out TransactionUpdateType>> =
        oldKeyboard
            .inline_keyboard
            .flatten()
            .filter { it.text.contains(TransactionUpdateType.pressedChar) }
            .mapNotNull { button ->
                TransactionUpdateType::class.sealedSubclasses.find {
                    button.text.contains(it.buttonWord())
                }
            }.toSet()

    private fun updateMarkupKeyboard(
        type: TransactionUpdateType,
        oldKeyboard: InlineKeyboardMarkup,
    ): InlineKeyboardMarkup =
        formatInlineKeyboard(pressedButtons(oldKeyboard) + type::class)
}
