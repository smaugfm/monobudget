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
import com.github.smaugfm.ynab.YnabTransactionDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mu.KotlinLogging
import java.text.DecimalFormat

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

    private suspend fun answerCallbackQuery(id: String, text: String?) {
        telegram.answerCallbackQuery(id, text)
    }

    suspend fun sendStatementMessage(
        event: Event.Telegram.SendStatementMessage,
    ) {
        val monoResponse = event.mono
        val transaction = event.transaction
        val telegramChatId = mappings.getTelegramChatIdAccByMono(monoResponse.account) ?: return
        val id = transaction.id

        telegram.sendMessage(
            telegramChatId,
            formatHTMLStatementMessage(monoResponse.statementItem, transaction),
            "HTML",
            markup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton(
                            "❌категорию",
                            callback_data = TransactionActionType.Uncategorize(id).serialize()
                        ),
                        InlineKeyboardButton("❌payee", callback_data = TransactionActionType.Unpayee(id).serialize()),
                        InlineKeyboardButton(
                            "\uD83D\uDEABreject",
                            callback_data = TransactionActionType.Unapprove(id).serialize()
                        ),
                    ),
                    listOf(
                        InlineKeyboardButton(
                            "➡️Невыясненные",
                            callback_data = TransactionActionType.Unknown(id).serialize()
                        ),
                        InlineKeyboardButton(
                            "➕payee",
                            callback_data = TransactionActionType.MakePayee(id, monoResponse.statementItem.description)
                                .serialize()
                        ),
                    )
                )
            )
        )
    }

    suspend fun handleCallbackQuery(
        dispatch: IEventDispatcher,
        event: Event.Telegram.CallbackQueryReceived,
    ) {
        val type = TransactionActionType.deserialize(event.data)
            ?: return Unit.also { telegram.answerCallbackQuery(event.callbackQueryId, UNKNOWN_ERROR_MSG) }
        logger.info("Deserialized callbackQuery to $type")

        dispatch(Event.Ynab.TransactionAction(type))
    }

    private fun formatHTMLStatementMessage(
        monoStatementItem: MonoStatementItem,
        transaction: YnabTransactionDetail,
    ): String {
        val builder = StringBuilder("Новая транзакция Monobank добавлена в YNAB\n")
        return with(monoStatementItem) {
            val format = DecimalFormat("##")
            val time = with(time.toLocalDateTime(TimeZone.currentSystemDefault())) {
                "${format.format(hour)}:${format.format(minute)}"
            }

            builder.append("\uD83D\uDCB3 <b>$description</b>                  ${time}\n")
            builder.append("      ${MCC.mapRussian[mcc] ?: "Неизвестный MCC:"}\n")
            builder.append("      <u>${currencyCode.formatAmount(amount)}${currencyCode.currencyCode}</u>\n")
            builder.append("      <code>Category: ${transaction.category_name}</code>\n")
            builder.append("      <code>Payee:    ${transaction.payee_name}</code>\n")
            builder.append("\n")

            builder.toString()
        }
    }

    companion object {
        const val UNKNOWN_ERROR_MSG = "Произошла непредвиденная ошибка."
    }
}
