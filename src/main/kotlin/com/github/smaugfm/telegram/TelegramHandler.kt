package com.github.smaugfm.telegram

import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.events.IEventsHandlerRegistrar
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.util.replaceNewLines
import com.github.smaugfm.ynab.YnabTransactionDetail
import mu.KotlinLogging
import java.util.UUID
import java.util.regex.Pattern

private val logger = KotlinLogging.logger {}

class TelegramHandler(
    private val telegram: TelegramApi,
    val mappings: Mappings,
) : IEventsHandlerRegistrar {
    override fun registerEvents(builder: HandlersBuilder) {
        builder.apply {
            registerUnit(this@TelegramHandler::sendStatementMessage)
            registerUnit(this@TelegramHandler::handleCallbackQuery)
        }
    }

    suspend fun sendStatementMessage(
        event: Event.Telegram.SendStatementMessage,
    ) {
        val monoResponse = event.mono
        val transaction = event.transaction
        val telegramChatId = mappings.getTelegramChatIdAccByMono(monoResponse.account) ?: return

        telegram.sendMessage(
            telegramChatId,
            formatHTMLStatementMessage(monoResponse.statementItem, transaction, transaction.id),
            "HTML",
            markup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            "❌категорию",
                            callback_data = TransactionActionType.Uncategorize.serialize()
                        ),
                        InlineKeyboardButton(
                            "\uD83D\uDEABunapprove",
                            callback_data = TransactionActionType.Unapprove.serialize()
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton(
                            "➡️Невыясненные",
                            callback_data = TransactionActionType.Unknown.serialize()
                        ),
                        InlineKeyboardButton(
                            "➕payee",
                            callback_data = TransactionActionType.MakePayee.serialize()
                        ),
                    )
                )
            )
        )
    }

    private suspend fun handleCallbackQuery(
        dispatch: IEventDispatcher,
        event: Event.Telegram.CallbackQueryReceived,
    ) {
        val (payee, transactionId) = extractPayeeAndTransactionIdFromMessage(event.messageText)

        if (transactionId == null) {
            logger.error(
                "Invalid message received. " +
                    "Cannot find transaction id.\nMessage:\n${event.messageText}"
            )
            return Unit.also { telegram.answerCallbackQuery(event.callbackQueryId, UNKNOWN_ERROR_MSG) }
        }

        val type = TransactionActionType.deserialize(event.data)
            ?: return Unit.also { telegram.answerCallbackQuery(event.callbackQueryId, UNKNOWN_ERROR_MSG) }

        logger.info("Deserialized callbackQuery to $type")

        dispatch(Event.Ynab.TransactionAction(payee, transactionId, type)).also {
            telegram.answerCallbackQuery(event.callbackQueryId)
        }
    }

    fun extractPayeeAndTransactionIdFromMessage(text: String): Pair<String?, String?> {
        val payeeRegex = Regex("")
        payeeRegex.javaClass.getDeclaredField("nativePattern").let {
            it.isAccessible = true
            it.set(
                payeeRegex,
                Pattern.compile("Payee:\\s+([\\w \t]+)$", Pattern.UNICODE_CHARACTER_CLASS or Pattern.MULTILINE)
            )
        }

        val payee = payeeRegex.find(text)?.groupValues?.get(1)
        val id = try {
            UUID.fromString(text.substring(text.length - UUIDwidth, text.length))
        } catch (e: IllegalArgumentException) {
            null
        }?.toString()

        return Pair(payee, id)
    }

    fun formatHTMLStatementMessage(
        monoStatementItem: MonoStatementItem,
        transaction: YnabTransactionDetail,
        id: String,
    ): String {
        val builder = StringBuilder("Новая транзакция Monobank добавлена в YNAB\n")
        return with(monoStatementItem) {
            builder.append("\uD83D\uDCB3 <b>${description.replaceNewLines()}</b>\n")
            builder.append("      ${MCC.mapRussian[mcc] ?: "Неизвестный MCC"} ($mcc)\n")
            builder.append("      <u>${currencyCode.formatAmount(amount)}${currencyCode.currencyCode}</u>\n")
            builder.append("      <code>Category: ${transaction.category_name ?: ""}</code>\n")
            builder.append("      <code>Payee:    ${transaction.payee_name ?: ""}</code>\n")
            builder.append("\n\n")
            builder.append("<pre>$id</pre>")

            builder.toString()
        }
    }

    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
        private const val UUIDwidth = 36
    }
}
