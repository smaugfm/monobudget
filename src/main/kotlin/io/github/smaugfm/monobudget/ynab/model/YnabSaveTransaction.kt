package io.github.smaugfm.monobudget.ynab.model

import io.github.smaugfm.monobudget.common.model.serializer.LocalDateAsISOSerializer
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
    val subtransactions: List<YnabSaveSubTransaction>,
)
