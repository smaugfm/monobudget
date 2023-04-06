package io.github.smaugfm.monobudget.ynab.model

import io.github.smaugfm.monobudget.common.model.serializer.LocalDateAsISOSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class YnabTransactionDetail(
    val id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val memo: String?,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flagColor: YnabFlagColor?,
    val accountId: String,
    val payeeId: String?,
    val categoryId: String?,
    val transferAccountId: String?,
    val transferTransactionId: String?,
    val matchedTransactionId: String?,
    val importId: String?,
    val deleted: Boolean,
    val accountName: String,
    val payeeName: String?,
    val categoryName: String?,
    val subtransactions: List<YnabSubTransaction>
) {
    fun toSaveTransaction(): YnabSaveTransaction {
        val t = this
        return YnabSaveTransaction(
            t.accountId,
            t.date,
            t.amount,
            t.payeeId,
            null,
            t.categoryId,
            t.memo,
            t.cleared,
            t.approved,
            t.flagColor,
            t.importId,
            t.subtransactions.map {
                YnabSaveSubTransaction(
                    it.amount,
                    it.payeeId,
                    it.payeeName,
                    it.categoryId,
                    it.memo
                )
            }
        )
    }
}
