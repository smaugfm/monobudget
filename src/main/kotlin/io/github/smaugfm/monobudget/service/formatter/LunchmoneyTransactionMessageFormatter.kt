package io.github.smaugfm.monobudget.service.formatter

import io.github.smaugfm.lunchmoney.model.LunchmoneyTransaction
import io.github.smaugfm.monobank.model.MonoStatementItem
import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.models.telegram.MessageWithReplyKeyboard
import io.github.smaugfm.monobudget.service.mono.MonoAccountsService
import io.github.smaugfm.monobudget.service.suggesting.CategorySuggestingService
import io.github.smaugfm.monobudget.util.MCC
import io.github.smaugfm.monobudget.util.replaceNewLines
import java.util.Currency

class LunchmoneyTransactionMessageFormatter(
    private val monoAccountsService: MonoAccountsService,
    private val categorySuggestingService: CategorySuggestingService,
) : TransactionMessageFormatter<LunchmoneyTransaction>() {
    override suspend fun format(
        monoResponse: MonoWebhookResponseData,
        transaction: LunchmoneyTransaction
    ): MessageWithReplyKeyboard {
        val msg = formatHTMLStatementMessage(
            monoAccountsService.getMonoAccAlias(monoResponse.account)!!,
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

    private suspend fun formatHTMLStatementMessage(
        accountAlias: String,
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
                categorySuggestingService.categoryNameById(transaction.categoryId.toString()) ?: "",
                transaction.payee,
                transaction.id.toString()
            )
        }
    }
}
