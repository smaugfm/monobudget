package io.github.smaugfm.monobudget.components.formatter

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.lunchmoney.model.enumeration.LunchmoneyTransactionStatus
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.components.suggestion.CategorySuggestionService
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
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
                transaction.id.toString()
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
}
