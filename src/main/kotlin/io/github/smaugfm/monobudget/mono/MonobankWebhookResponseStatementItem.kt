package io.github.smaugfm.monobudget.mono

import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.model.financial.StatementItem

class MonobankWebhookResponseStatementItem(
    original: MonoWebhookResponseData
) : StatementItem {
    override val id = original.statementItem.id
    override val accountId = original.account
    override val time = original.statementItem.time
    override val description = original.statementItem.description
    override val comment = original.statementItem.comment
    override val mcc = original.statementItem.mcc
    override val amount = original.statementItem.amount
    override val operationAmount = original.statementItem.operationAmount
    override val currency = original.statementItem.currencyCode
}
