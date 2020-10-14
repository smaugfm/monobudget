package com.github.smaugfm.handlers

import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TelegramApi
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.ynab.YnabTransactionDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.DecimalFormat
import java.util.logging.Logger

class TelegramHandler(
    private val telegram: TelegramApi,
    mappings: Mappings,
) : EventHandlerBase(mappings) {
    private val logger = Logger.getLogger(TelegramHandler::class.qualifiedName.toString())

    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        when (e) {
            is Event.Telegram.SendStatementMessage -> sendStatementMessage(
                e.mono,
                e.transaction,
            )
            is Event.Telegram.CallbackQueryReceived -> handleCallbackQuery(dispatch, e.telegramChatId, e.data)
            else -> return false
        }

        return true
    }

    suspend fun sendStatementMessage(
        monoResponse: MonoWebHookResponseData,
        transaction: YnabTransactionDetail,
    ) {
        val telegramChatId = mappings.getTelegramChatIdAccByMono(monoResponse.account) ?: return
        val data = callbackData(transaction.id)

        telegram.sendMessage(
            telegramChatId,
            formatHTMLStatementMessage(monoResponse.statementItem, transaction),
            "HTML",
            markup = InlineKeyboardMarkup(
                listOf(
                    listOf(
                        InlineKeyboardButton("Unclear", callback_data = data(UpdateType.Unclear)),
                        InlineKeyboardButton("Mark red", callback_data = data(UpdateType.MarkRed)),
                        InlineKeyboardButton("Невыясненные", callback_data = data(UpdateType.Unrecognized)),
                    )
                )
            )
        )
    }

    suspend fun handleCallbackQuery(dispatch: Dispatch, chatId: Int, data: String) {
        val matchResult = callbackDataPattern.matchEntire(data)
        if (matchResult == null) {
            logger.severe("Callback query callback_data does not match pattern. $data")
            sendUnknownErrorMessage(chatId)
            return
        }

        val typeStr = matchResult.groupValues[1]
        val transactionId = matchResult.groupValues[2]
        val type = try {
            UpdateType.valueOf(typeStr)
        } catch (e: Throwable) {
            logger.severe("Callback query callback_data does not contain known CallbackType. $data")
            sendUnknownErrorMessage(chatId)
            return
        }

        dispatch(Event.Ynab.UpdateTransaction(transactionId, type))
    }

    private suspend fun sendUnknownErrorMessage(chatId: Int) {
        telegram.sendMessage(chatId, unknownErrorMessage)
    }

    private fun formatHTMLStatementMessage(
        monoStatementItem: MonoStatementItem,
        transaction: YnabTransactionDetail,
    ): String {
        val builder = StringBuilder("Новая транзакция Monobank добавлена в YNAB\n")
        return with(monoStatementItem) {
            val format = DecimalFormat("##")
            val time = with(time.toLocalDateTime(TimeZone.currentSystemDefault())) {
                "${format.format(hour)}:${
                format.format(minute)
                }"
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
        enum class UpdateType {
            Unclear,
            MarkRed,
            Unrecognized
        }

        const val unknownErrorMessage = "Произошла непредвиденная ошибка."

        private val callbackDataPattern = Regex("(\\S+)   (\\S+)")

        fun callbackData(transactionId: String) = { updateType: UpdateType ->
            "$updateType   $transactionId"
        }
    }
}