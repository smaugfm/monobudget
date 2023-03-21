package io.github.smaugfm.monobudget.service.formatter

import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.models.ynab.YnabTransactionDetail
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
import java.util.Currency

class YnabTransactionMessageFormatter(
    private val monoAccountsService: MonoAccountsService,
) : TransactionMessageFormatter<YnabTransactionDetail>() {
    override suspend fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: YnabTransactionDetail
    ): MessageWithReplyKeyboard {
        val msg = formatHTMLStatementMessage(
            monoAccountsService.getAccountCurrency(monoResponse.account)!!,
            monoResponse.statementItem,
            transaction
        )
        val markup = formatInlineKeyboard(emptySet())

        return MessageWithReplyKeyboard(
            msg,
            markup
        )
    }

    companion object {
        private fun formatHTMLStatementMessage(
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
}
