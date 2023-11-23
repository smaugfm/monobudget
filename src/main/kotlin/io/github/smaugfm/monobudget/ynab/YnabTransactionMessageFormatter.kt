package io.github.smaugfm.monobudget.ynab

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.monobudget.common.category.CategoryService
import io.github.smaugfm.monobudget.common.lifecycle.StatementProcessingScopeComponent
import io.github.smaugfm.monobudget.common.misc.MCC
import io.github.smaugfm.monobudget.common.model.callback.PressedButtons
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import io.github.smaugfm.monobudget.ynab.model.YnabCleared
import io.github.smaugfm.monobudget.ynab.model.YnabTransactionDetail
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import java.util.Currency

@Scoped
@Scope(StatementProcessingScopeComponent::class)
class YnabTransactionMessageFormatter(
    private val categoryService: CategoryService,
    statementItem: StatementItem,
) : TransactionMessageFormatter<YnabTransactionDetail>(statementItem) {
    override suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        transaction: YnabTransactionDetail,
    ): String {
        with(statementItem) {
            val category = categoryService.budgetedCategoryById(transaction.categoryId)

            return formatHTMLStatementMessage(
                "YNAB",
                (description ?: "").replaceNewLines(),
                (MCC.map[mcc]?.fullDescription ?: "Невідомий MCC") + " ($mcc)",
                "$amount${(if (accountCurrency != currency) " ($operationAmount)" else "")}",
                category,
                transaction.payeeName ?: "",
                transaction.id,
            )
        }
    }

    override fun shouldNotify(transaction: YnabTransactionDetail): Boolean =
        transaction.categoryId == null || transaction.cleared == YnabCleared.Uncleared

    override fun getReplyKeyboardPressedButtons(
        transaction: YnabTransactionDetail,
        callbackType: TransactionUpdateType?,
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

    override fun getReplyKeyboard(pressed: PressedButtons) =
        InlineKeyboardMarkup(
            listOf(
                listOf(
                    TransactionUpdateType.Unapprove.button(pressed),
                    TransactionUpdateType.Uncategorize.button(pressed),
                    TransactionUpdateType.MakePayee.button(pressed),
                ),
            ),
        )
}
