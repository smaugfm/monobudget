package com.github.smaugfm.ynab

import com.github.smaugfm.serializers.LocalDateAsISOSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class YnabSaveTransaction(
    val account_id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val payee_id: String,
    val payee_name: String,
    val category_id: String,
    val memo: String,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flag_color: YnabFlagColor,
    val import_id: String,
    val subtransactions: List<YnabSaveSubTransaction>,
)

@Serializable
data class YnabSaveSubTransaction(
    val amount: Long,
    val payee_id: String,
    val payee_name: String,
    val category_id: String,
    val memo: String,
)

@Serializable
data class YnabSaveTransactionWrapper(
    val transaction: YnabSaveTransaction,
)

@Serializable
data class YnabTransactionResponse(
    val data: YnabTransactionWrapper,
)

@Serializable
data class YnabTransactionWrapper(
    val transaction: YnabTransactionDetail,
)

@Serializable
data class YnabTransactionDetail(
    val id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val cleared: YnabCleared,
    val approved: Boolean,
    val accountId: String,
    val deleted: Boolean,
    val accountName: String,
    val subtransactions: List<YnabSubTransaction>,
)

@Serializable
data class YnabSubTransaction(
    val id: String,
    val transactionI: String,
    val amount: Long,
    val delete: Boolean,
)