package io.github.smaugfm.monobudget.components.formatter

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.model.callback.PressedButtons
import io.github.smaugfm.monobudget.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.model.ynab.YnabCleared
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
import java.util.Currency

class YnabTransactionMessageFormatter : TransactionMessageFormatter<YnabTransactionDetail>() {

    override suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        monoStatementItem: MonoStatementItem,
        transaction: YnabTransactionDetail
    ): String {
        with(monoStatementItem) {
            val accountAmount = formatAmountWithCurrency(amount, accountCurrency)
            val operationAmount = formatAmountWithCurrency(this.operationAmount, currencyCode)
            return formatHTMLStatementMessage(
                "YNAB",
                (description ?: "").replaceNewLines(),
                (MCC.map[mcc]?.fullDescription ?: "Невідомий MCC") + " ($mcc)",
                accountAmount + (if (accountCurrency != currencyCode) " ($operationAmount)" else ""),
                transaction.categoryName ?: "",
                transaction.payeeName ?: "",
                transaction.id
            )
        }
    }

    override fun shouldNotify(transaction: YnabTransactionDetail): Boolean =
        transaction.categoryId == null || transaction.cleared == YnabCleared.Uncleared

    override fun getReplyKeyboardPressedButtons(
        transaction: YnabTransactionDetail,
        callbackType: TransactionUpdateType?
    ): PressedButtons {
        val pressed = PressedButtons(callbackType)

        if (transaction.categoryName.isNullOrEmpty()) {
            pressed(TransactionUpdateType.Uncategorize::class)
        }
        if (transaction.cleared == YnabCleared.Uncleared) {
            pressed(TransactionUpdateType.Unapprove::class)
        }

        return pressed
    }

    override fun getReplyKeyboard(transaction: YnabTransactionDetail, pressed: PressedButtons) = InlineKeyboardMarkup(
        listOf(
            listOf(
                TransactionUpdateType.Unapprove.button(pressed),
                TransactionUpdateType.Uncategorize.button(pressed),
                TransactionUpdateType.MakePayee.button(pressed)
            )
        )
    )
}
