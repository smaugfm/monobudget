package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.model.financial.StatementItem
import java.util.Currency

data class MonobankWebhookResponseStatementItem(
    val d: MonoWebhookResponseData,
    val accountCurrency: Currency
) : StatementItem {
    override val id = d.statementItem.id
    override val accountId = d.account
    override val time = d.statementItem.time
    override val description = d.statementItem.description
    override val comment = d.statementItem.comment
    override val mcc = d.statementItem.mcc
    override val amount =
        Amount(d.statementItem.amount, accountCurrency)
    override val operationAmount =
        Amount(d.statementItem.operationAmount, d.statementItem.currencyCode)
    override val currency = d.statementItem.currencyCode
}
