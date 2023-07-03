package io.github.smaugfm.monobudget.ynab

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.misc.MCC
import io.github.smaugfm.monobudget.common.model.callback.PressedButtons
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import io.github.smaugfm.monobudget.ynab.model.YnabCleared
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Single
import java.util.Currency

@Single
class YnabTransactionMessageFormatter(
    private val categoryService: CategoryService
) : TransactionMessageFormatter<YnabTransactionDetail>() {

    override suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        statementItem: StatementItem,
        transaction: YnabTransactionDetail
    ): String {
        with(statementItem) {
            val accountAmount = amount.formatWithCurrency(accountCurrency)
            val operationAmount = operationAmount.formatWithCurrency(currency)

            val category = categoryService.budgetedCategoryById(transaction.categoryId)

            return formatHTMLStatementMessage(
                "YNAB",
                (description ?: "").replaceNewLines(),
                (MCC.map[mcc]?.fullDescription ?: "Невідомий MCC") + " ($mcc)",
                accountAmount + (if (accountCurrency != currency) " ($operationAmount)" else ""),
                category,
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

    override fun getReplyKeyboard(transaction: YnabTransactionDetail, pressed: PressedButtons) =
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    TransactionUpdateType.Unapprove.button(pressed),
                    TransactionUpdateType.Uncategorize.button(pressed),
                    TransactionUpdateType.MakePayee.button(pressed)
                )
            )
        )
}
