package io.github.smaugfm.monobudget.common.model.financial

import kotlinx.datetime.Instant
import java.util.Currency

interface StatementItem {
    val id: String
    val accountId: String
    val time: Instant
    val description: String?
    val comment: String?
    val mcc: Int
    val amount: Long
    val operationAmount: Long
    val currency: Currency
}
