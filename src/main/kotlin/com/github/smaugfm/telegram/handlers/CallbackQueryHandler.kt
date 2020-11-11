package com.github.smaugfm.telegram.handlers

import com.elbekD.bot.types.InlineKeyboardMarkup
import com.elbekD.bot.types.Message
import com.elbekD.bot.types.MessageEntity
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.events.IEventsHandlerRegistrar
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.telegram.TransactionActionType.Companion.buttonWord
import com.github.smaugfm.ynab.YnabTransactionDetail
import mu.KotlinLogging
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class CallbackQueryHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : IEventsHandlerRegistrar {
    override fun registerEvents(builder: HandlersBuilder) {
        builder.apply {
            registerUnit(this@CallbackQueryHandler::handle)
        }
    }

    suspend fun handle(
        dispatch: IEventDispatcher,
        event: Event.Telegram.CallbackQueryReceived,
    ) {
        val type = TransactionActionType.deserialize(event.data, event.message)
            ?: return Unit.also {
                telegram.answerCallbackQuery(
                    event.callbackQueryId,
                    TelegramHandler.UNKNOWN_ERROR_MSG
                )
            }
        logger.info("Found callbackQuery action type $type")

        val updatedTransaction = dispatch(Event.Ynab.TransactionAction(type)).also {
            telegram.answerCallbackQuery(event.callbackQueryId)
        }

        val updatedText = updateHTMLStatementMessage(updatedTransaction, event.message)
        val updatedMarkup = updateMarkupKeyboard(type, event.message.reply_markup!!)

        if (stripHTMLTagsFromMessage(updatedText) != event.message.text ||
            updatedMarkup != event.message.reply_markup
        ) {
            with(event.message) {
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
        oldMessage: Message
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

    private fun pressedButtons(oldKeyboard: InlineKeyboardMarkup): Set<KClass<out TransactionActionType>> =
        oldKeyboard
            .inline_keyboard
            .flatten()
            .filter { it.text.contains(TransactionActionType.pressedChar) }
            .mapNotNull { button ->
                TransactionActionType::class.sealedSubclasses.find {
                    button.text.contains(it.buttonWord())
                }
            }.toSet()

    private fun updateMarkupKeyboard(
        type: TransactionActionType,
        oldKeyboard: InlineKeyboardMarkup
    ): InlineKeyboardMarkup =
        formatInlineKeyboard(pressedButtons(oldKeyboard) + type::class)
}
