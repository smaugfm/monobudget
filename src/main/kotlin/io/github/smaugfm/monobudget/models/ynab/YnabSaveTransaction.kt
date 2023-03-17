package io.github.smaugfm.monobudget.models.ynab

import io.github.smaugfm.monobudget.models.serializer.LocalDateAsISOSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransaction(
    val accountId: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val payeeId: String?,
    val payeeName: String?,
    val categoryId: String?,
    val memo: String?,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flagColor: YnabFlagColor?,
    val importId: String?,
    val subtransactions: List<YnabSaveSubTransaction>
)
