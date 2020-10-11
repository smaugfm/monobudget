package com.github.smaugfm.handlers

import com.github.smaugfm.apis.TelegramApi
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.ynab.YnabSaveTransaction
import kotlinx.coroutines.future.asDeferred
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.DecimalFormat

class TelegramHandler(
    private val telegram: TelegramApi,
) : EventHandlerBase() {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        if (e !is Event.Telegram)
            return false

        when (e) {
            is Event.Telegram.SendStatementMessage -> sendStatementMessage(e.telegramChatId,
                e.monoStatementItem,
                e.transaction,
                e.categoryName)
            is Event.Telegram.UnclearTransaction -> dispatch(Event.Ynab.MarkTransactionUncleared(TODO()))
            is Event.Telegram.MarkTransactionRed -> dispatch(Event.Ynab.MarkTransactionRed(TODO()))
        }

        return true
    }

    suspend fun sendStatementMessage(
        telegramChatId: Long,
        monoStatementItem: MonoStatementItem,
        transaction: YnabSaveTransaction,
        categoryName: String,
    ) {
        telegram.bot.sendMessage(
            telegramChatId,
            formatHTMLStatementMessage(monoStatementItem, transaction, categoryName),
            "HTML"
        ).asDeferred().await()
    }

    private fun formatHTMLStatementMessage(
        monoStatementItem: MonoStatementItem,
        transaction: YnabSaveTransaction,
        categoryName: String,
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
            builder.append("      <code>Category: $categoryName</code>\n")
            builder.append("      <code>Payee:    ${transaction.payee_name}</code>\n")
            builder.append("\n")

            builder.toString()
        }
    }
}