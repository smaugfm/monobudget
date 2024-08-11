package io.github.smaugfm.monobudget.import

import io.github.smaugfm.monobudget.common.model.financial.BankAccountId
import io.github.smaugfm.monobudget.common.model.serializer.CurrencyAsStringSerializer
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
internal data class CsvMonoItem(
    @Serializable(CsvMonoInstantSerializer::class)
    val date: Instant,
    val description: String,
    val mcc: Int,
    val cardCurrencyAmount: Double,
    val transactionCurrencyAmount: Double,
    @Serializable(CurrencyAsStringSerializer::class)
    val currency: Currency,
    val exchangeRate: Double?,
    val cardCurrencyCommissionAmount: Double?,
    val cardCurrencyCashbackAmount: Double?,
    val balance: Double?,
) {
    fun toStatementItem(
        accountId: BankAccountId,
        accountCurrency: Currency,
    ) = ImporterStatementItem(this, accountId, accountCurrency)
}
