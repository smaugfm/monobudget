package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.model.financial.Amount
import io.github.smaugfm.monobudget.common.model.financial.StatementItem

data class MonobankWebhookResponseStatementItem(
    val original: MonoWebhookResponseData
) : StatementItem {
    override val id = original.statementItem.id
    override val accountId = original.account
    override val time = original.statementItem.time
    override val description = original.statementItem.description
    override val comment = original.statementItem.comment
    override val mcc = original.statementItem.mcc
    override val amount = Amount(original.statementItem.amount)
    override val operationAmount = Amount(original.statementItem.operationAmount)
    override val currency = original.statementItem.currencyCode
}
