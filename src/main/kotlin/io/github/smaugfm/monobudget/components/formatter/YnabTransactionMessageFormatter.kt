package io.github.smaugfm.monobudget.components.formatter

import com.elbekd.bot.types.InlineKeyboardMarkup
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.model.TransactionUpdateType
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
import java.util.Currency
import kotlin.reflect.KClass

class YnabTransactionMessageFormatter(
    monoAccountsService: MonoAccountsService
) : TransactionMessageFormatter<YnabTransactionDetail>(monoAccountsService) {

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

    override fun getReplyKeyboard(): InlineKeyboardMarkup {
        val pressed: Set<KClass<out TransactionUpdateType>> = emptySet()
        return InlineKeyboardMarkup(
            listOf(
                listOf(
                    button<TransactionUpdateType.Unapprove>(pressed),
                    button<TransactionUpdateType.Uncategorize>(pressed),
                    button<TransactionUpdateType.MakePayee>(pressed)
                )
            )
        )
    }
}
