package com.github.smaugfm.telegram

import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.EventHandlerBase
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.ynab.YnabTransactionDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.DecimalFormat

class TelegramHandler(
    private val telegram: TelegramApi,
    mappings: Mappings,
) : EventHandlerBase(mappings) {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        when (e) {
            is Event.Telegram.SendStatementMessage -> sendStatementMessage(
                e.mono,
                e.transaction,
            )
            is Event.Telegram.CallbackQueryReceived -> {
                val responseText = handleCallbackQuery(dispatch, e.data)
                dispatch(Event.Telegram.AnswerCallbackQuery(e.callbackQueryId, responseText))
            }
            is Event.Telegram.AnswerCallbackQuery -> {
                answerCallbackQuery(e.callbackQueryId, e.text)
            }
            else -> return false
        }

        return true
    }

    suspend fun answerCallbackQuery(id: String, text: String?) {
        telegram.answerCallbackQuery(id, text)
    }

    suspend fun sendStatementMessage(
        monoResponse: MonoWebHookResponseData,
        transaction: YnabTransactionDetail,
    ) {
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

    suspend fun handleCallbackQuery(dispatch: Dispatch, data: String): String? {
        val type = TransactionActionType.deserialize(data) ?: return unknownErrorMessage

        dispatch(Event.Ynab.TransactionAction(type))
        return null
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
        const val unknownErrorMessage = "Произошла непредвиденная ошибка."
    }
}
