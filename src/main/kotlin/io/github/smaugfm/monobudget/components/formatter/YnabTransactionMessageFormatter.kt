package io.github.smaugfm.monobudget.components.formatter

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobudget.model.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.components.mono.MonoAccountsService
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
import java.util.Currency

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
}
