package io.github.smaugfm.monobudget.import

import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import java.util.Currency
import java.util.UUID

internal data class ImporterStatementItem(
    val csv: CsvMonoItem,
    override val accountId: BankAccountId,
    val accountCurrency: Currency,
) : StatementItem {
    override val id = UUID.randomUUID().toString()
    override val time = csv.date
    override val description = csv.description
    override val comment = null
    override val mcc = csv.mcc
    override val amount =
        Amount.fromLunchmoneyAmount(
            csv.cardCurrencyAmount,
            accountCurrency,
        )
    override val operationAmount =
        Amount.fromLunchmoneyAmount(
            csv.transactionCurrencyAmount,
            csv.currency,
        )
    override val currency = csv.currency
}
