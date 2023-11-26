package io.github.smaugfm.monobudget.common.model.financial

import com.fasterxml.jackson.annotation.JsonTypeInfo
import kotlinx.datetime.Instant
import java.util.Currency

@JsonTypeInfo(
    use = JsonTypeInfo.Id.MINIMAL_CLASS,
    include = JsonTypeInfo.As.PROPERTY,
)
interface StatementItem {
    val id: String
    val accountId: BankAccountId
    val time: Instant
    val description: String?
    val comment: String?
    val mcc: Int
    val amount: Amount
    val operationAmount: Amount
    val currency: Currency

    fun formatAmount() = amount.format()
}
