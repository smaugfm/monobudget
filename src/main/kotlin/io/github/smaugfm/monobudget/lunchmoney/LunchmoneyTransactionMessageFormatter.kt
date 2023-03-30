package io.github.smaugfm.monobudget.lunchmoney

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.common.CategorySuggestionService
import io.github.smaugfm.monobudget.common.misc.MCC
import io.github.smaugfm.monobudget.common.model.callback.ActionCallbackType
import io.github.smaugfm.monobudget.common.model.callback.PressedButtons
import io.github.smaugfm.monobudget.common.model.callback.TransactionUpdateType
import io.github.smaugfm.monobudget.common.transaction.TransactionMessageFormatter
import io.github.smaugfm.monobudget.common.util.formatW
import io.github.smaugfm.monobudget.common.util.replaceNewLines
import io.github.smaugfm.monobudget.common.util.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import org.koin.core.component.inject
import java.util.Currency

class LunchmoneyTransactionMessageFormatter : TransactionMessageFormatter<LunchmoneyTransaction>() {
    private val categorySuggestingService: CategorySuggestionService by inject()

    private val shouldNotifyStatuses = setOf(
        LunchmoneyTransactionStatus.UNCLEARED,
        LunchmoneyTransactionStatus.RECURRING_SUGGESTED,
        LunchmoneyTransactionStatus.PENDING

    )

    override suspend fun formatHTMLStatementMessage(
        accountCurrency: Currency,
        monoStatementItem: MonoStatementItem,
        transaction: LunchmoneyTransaction
    ): String {
        with(monoStatementItem) {
            val accountAmount = formatAmountWithCurrency(amount, accountCurrency)
            val operationAmount = formatAmountWithCurrency(this.operationAmount, currencyCode)
            return formatHTMLStatementMessage(
                "Lunchmoney",
                (description ?: "").replaceNewLines(),
                (MCC.map[mcc]?.fullDescription ?: "Невідомий MCC") + " ($mcc)",
                accountAmount + (if (accountCurrency != currencyCode) " ($operationAmount)" else ""),
                categorySuggestingService.categoryNameById(transaction.categoryId?.toString()) ?: "",
                transaction.payee,
                transaction.id.toString(),
                constructTransactionsQuickUrl()
            )
        }
    }

    override fun shouldNotify(transaction: LunchmoneyTransaction): Boolean =
        transaction.categoryId == null || transaction.status in shouldNotifyStatuses

    override fun getReplyKeyboardPressedButtons(
        transaction: LunchmoneyTransaction,
        callbackType: TransactionUpdateType?
    ): PressedButtons {
        val pressed = PressedButtons(callbackType)

        if (transaction.categoryId == null) {
            pressed(TransactionUpdateType.Uncategorize::class)
        }
        if (transaction.status == LunchmoneyTransactionStatus.UNCLEARED) {
            pressed(TransactionUpdateType.Unapprove::class)
        }

        return pressed
    }

    override fun getReplyKeyboard(transaction: LunchmoneyTransaction, pressed: PressedButtons) = InlineKeyboardMarkup(
        listOf(
            listOf(
                TransactionUpdateType.Unapprove.button(pressed),
                ActionCallbackType.ChooseCategory.button(pressed)
            )
        )
    )

    companion object {
        fun constructTransactionsQuickUrl(date: LocalDate = Clock.System.now().toLocalDateTime().date): String {
            val monthNumber = date.month.value.formatW()
            return "${LunchmoneyApi.LUNCHMONEY_APP_BASE_URL}/transactions/${date.year}/$monthNumber"
        }
    }
}
