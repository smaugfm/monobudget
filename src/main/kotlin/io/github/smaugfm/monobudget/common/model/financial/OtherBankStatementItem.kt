package io.github.smaugfm.monobudget.common.model.financial

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.Currency
import java.util.UUID

data class OtherBankStatementItem(
    override val id: String = UUID.randomUUID().toString(),
    override val accountId: String,
    override val time: Instant = Clock.System.now(),
    override val description: String?,
    override val comment: String? = null,
    override val mcc: Int,
    override val amount: Amount,
    override val operationAmount: Amount,
    override val currency: Currency
) : StatementItem
