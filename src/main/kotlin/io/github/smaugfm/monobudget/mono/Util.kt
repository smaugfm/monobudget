import io.github.smaugfm.monobank.model.MonoWebhookResponseData
import io.github.smaugfm.monobudget.common.model.financial.StatementItem

fun MonoWebhookResponseData.toStatementItem() = with(statementItem) {
    StatementItem(
        id,
        account,
        time,
        description,
        comment,
        mcc,
        amount,
        operationAmount,
        currencyCode
    )
}
