package com.github.smaugfm.telegram.handlers

import com.elbekD.bot.types.InlineKeyboardButton
import com.elbekD.bot.types.InlineKeyboardMarkup
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.telegram.TransactionActionType.Companion.buttonText
import com.github.smaugfm.telegram.TransactionActionType.Companion.serialize
import com.github.smaugfm.util.MCC
import com.github.smaugfm.util.formatAmount
import com.github.smaugfm.util.replaceNewLines
import com.github.smaugfm.ynab.YnabTransactionDetail
import java.util.Currency
import kotlin.reflect.KClass

internal fun formatInlineKeyboard(
    pressed: Set<KClass<out TransactionActionType>>,
): InlineKeyboardMarkup {
    return InlineKeyboardMarkup(
        listOf(
            listOf(
                button<TransactionActionType.Uncategorize>(pressed),
                button<TransactionActionType.Unapprove>(pressed),
            ),
            listOf(
                button<TransactionActionType.Unknown>(pressed),
                button<TransactionActionType.MakePayee>(pressed),
            )
        )
    )
}

internal fun stripHTMLTagsFromMessage(messageText: String): String {
    val replaceHtml = Regex("<.*?>")
    return replaceHtml.replace(messageText, "")
}

internal fun formatHTMLStatementMessage(
    description: String,
    mcc: String,
    amount: String,
    category: String,
    payee: String,
    id: String,
): String {
    val builder = StringBuilder("Новая транзакция Monobank добавлена в YNAB\n")
    return with(Unit) {
        builder.append("\uD83D\uDCB3 <b>$description</b>\n")
        builder.append("      $mcc\n")
        builder.append("      <u>$amount</u>\n")
        builder.append("      <code>Category: $category</code>\n")
        builder.append("      <code>Payee:    $payee</code>\n")
        builder.append("\n\n")
        builder.append("<pre>$id</pre>")

        builder.toString()
    }
}

internal fun formatAmountWithCurrency(amount: Long, currency: Currency) =
    currency.formatAmount(amount) + currency.currencyCode

internal fun formatHTMLStatementMessage(
    accountCurrency: Currency,
    monoStatementItem: MonoStatementItem,
    transaction: YnabTransactionDetail,
): String {
    with(monoStatementItem) {
        val accountAmount = formatAmountWithCurrency(amount, accountCurrency)
        val operationAmount = formatAmountWithCurrency(this.operationAmount, currencyCode)
        return formatHTMLStatementMessage(
            description.replaceNewLines(),
            (MCC.mapRussian[mcc] ?: "Неизвестный MCC") + " ($mcc)",
            accountAmount + (if (accountCurrency != currencyCode) " ($operationAmount)" else ""),
            transaction.category_name ?: "",
            transaction.payee_name ?: "",
            transaction.id,
        )
    }
}

internal inline fun <reified T : TransactionActionType> button(pressed: Set<KClass<out TransactionActionType>>) =
    with(T::class) {
        InlineKeyboardButton(
            buttonText<T>(this in pressed),
            callback_data = serialize<T>()
        )
    }

suspend fun errorHandler(
    dispatcher: IEventDispatcher,
    mappings: Mappings,
) {
    mappings
        .getTelegramChatIds()
        .forEach { chatId ->
            dispatcher(
                Event.Telegram.SendHTMLMessage(
                    chatId,
                    TelegramHandler.UNKNOWN_ERROR_MSG,
                    null
                )
            )
        }
}
