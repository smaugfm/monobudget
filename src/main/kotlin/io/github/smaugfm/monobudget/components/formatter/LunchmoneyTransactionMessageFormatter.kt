package io.github.smaugfm.monobudget.components.formatter

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.lunchmoney.api.LunchmoneyApi
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.formatW
import io.github.smaugfm.monobudget.util.replaceNewLines
import io.github.smaugfm.monobudget.util.toLocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import java.util.Currency
import kotlin.reflect.KClass

class LunchmoneyTransactionMessageFormatter(
    monoAccountsService: MonoAccountsService,
    private val categorySuggestingService: CategorySuggestionService
) : TransactionMessageFormatter<LunchmoneyTransaction>(monoAccountsService) {

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
        updateType: TransactionUpdateType?
    ): Set<KClass<out TransactionUpdateType>> {
        val pressed: MutableSet<KClass<out TransactionUpdateType>> =
            updateType?.let { mutableSetOf(it::class) } ?: mutableSetOf()

        if (transaction.categoryId == null) {
            pressed.add(TransactionUpdateType.Uncategorize::class)
        }
        if (transaction.status == LunchmoneyTransactionStatus.UNCLEARED) {
            pressed.add(TransactionUpdateType.Unapprove::class)
        }

        return pressed
    }

    override fun getReplyKeyboard(
        transaction: LunchmoneyTransaction,
        pressed: Set<KClass<out TransactionUpdateType>>
    ): InlineKeyboardMarkup {
        return InlineKeyboardMarkup(
            listOf(
                listOf(
                    button<TransactionUpdateType.Unapprove>(pressed),
                    button<TransactionUpdateType.Uncategorize>(pressed)
                )
            )
        )
    }

    companion object {
        fun constructTransactionsQuickUrl(date: LocalDate = Clock.System.now().toLocalDateTime().date): String {
            val monthNumber = date.month.value.formatW()
            return "${LunchmoneyApi.LUNCHMONEY_APP_BASE_URL}/transactions/${date.year}/$monthNumber"
        }
    }
}
