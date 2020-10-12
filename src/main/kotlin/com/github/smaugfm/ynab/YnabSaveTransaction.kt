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
data class YnabSaveTransactionResponse(
    val data: YnabSaveTransactionDetailWrapper,
)

@Serializable
data class YnabTransactionResponse(
    val data: YnabTransactionDetailWrapper
)

@Serializable
data class YnabSaveTransactionDetailWrapper(
    val transaction: YnabTransactionDetail,
    val transaction_ids: List<String>,
    val server_knowledge: Long
)

@Serializable
data class YnabTransactionDetailWrapper(
    val transaction: YnabTransactionDetail,
)

@Serializable
data class YnabTransactionDetail(
    val id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val memo: String,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flag_color: YnabFlagColor,
    val accountId: String,
    val payee_id: String,
    val category_id: String?,
    val transfer_account_id: String,
    val transfer_transaction_id: String,
    val matched_transaction_id: String,
    val import_id: String,
    val deleted: Boolean,
    val account_name: String,
    val payee_name: String,
    val category_name: String,
    val subtransactions: List<YnabSubTransaction>,
)

@Serializable
data class YnabSubTransaction(
    val id: String,
    val transaction_id: String,
    val amount: Long,
    val memo: String,
    val payee_id: String,
    val payee_name: String,
    val category_id: String,
    val category_name: String,
    val transfer_account_id: String,
    val transfer_transaction_id: String,
    val delete: Boolean,
)