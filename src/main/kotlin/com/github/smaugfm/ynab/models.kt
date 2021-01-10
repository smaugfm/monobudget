package com.github.smaugfm.ynab

import com.github.smaugfm.serializers.LocalDateAsISOSerializer
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class YnabCleared {
    @SerialName("cleared")
    Cleared,

    @SerialName("uncleared")
    Uncleared,

    @SerialName("reconciled")
    Reconciled
}

@Serializable
enum class YnabFlagColor {
    @SerialName("red")
    Red,

    @SerialName("orange")
    Orange,

    @SerialName("yellow")
    Yellow,

    @SerialName("green")
    Green,

    @SerialName("blue")
    Blue,

    @SerialName("purple")
    Purple,
}

@Serializable
data class YnabPayeesResponse(
    val data: YnabPayeesWrapper,
)

@Serializable
data class YnabPayeesWrapper(
    val payees: List<YnabPayee>,
    val server_knowledge: Long,
)

@Serializable
data class YnabPayee(
    val id: String,
    val name: String,
    val transfer_account_id: String?,
    val deleted: Boolean,
)

@Serializable
data class YnabCategoriesResponse(
    val data: YnabCategoryGroupsWithCategoriesWrapper,
)

@Serializable
data class YnabCategoryGroupsWithCategoriesWrapper(
    val category_groups: List<YnabCategoryGroupWithCategories>,
    val server_knowledge: Long,
)

@Serializable
data class YnabCategoryGroupWithCategories(
    val id: String,
    val name: String,
    val hidden: Boolean,
    val deleted: Boolean,
    val categories: List<YnabCategory>,
)

@Serializable
data class YnabCategory(
    val id: String,
    val category_group_id: String,
    val name: String,
    val hidden: Boolean,
    val original_category_group_id: String?,
    val note: String?,
    val budgeted: Long,
    val activity: Long,
    val balance: Long,
    val goal_type: String?,
    val goal_creation_month: String?,
    val goal_target: Long?,
    val goal_target_month: String?,
    val goal_percentage_complete: Int?,
    val deleted: Boolean,
)

@Serializable
data class YnabSaveTransaction(
    val account_id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val payee_id: String?,
    val payee_name: String?,
    val category_id: String?,
    val memo: String?,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flag_color: YnabFlagColor?,
    val import_id: String?,
    val subtransactions: List<YnabSaveSubTransaction>,
)

@Serializable
data class YnabSaveSubTransaction(
    val amount: Long,
    val payee_id: String?,
    val payee_name: String?,
    val category_id: String?,
    val memo: String?,
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
    val data: YnabTransactionDetailWrapper,
)

@Serializable
data class YnabTransactionResponseWithServerKnowledge(
    val data: YnabTransactionDetailWrapperWithServerKnowledge,
)

@Serializable
data class YnabTransactionsResponse(
    val data: YnabTransactionsDetailWrapper,
)

@Serializable
data class YnabSaveTransactionDetailWrapper(
    val transaction: YnabTransactionDetail,
    val transaction_ids: List<String>,
    val server_knowledge: Long,
)

@Serializable
data class YnabTransactionDetailWrapper(
    val transaction_ids: List<String>,
    val transaction: YnabTransactionDetail,
)

@Serializable
data class YnabTransactionDetailWrapperWithServerKnowledge(
    val transaction_ids: List<String>,
    val transaction: YnabTransactionDetail,
    val server_knowledge: Long,
)

@Serializable
data class YnabTransactionsDetailWrapper(
    val transactions: List<YnabTransactionDetail>,
    val server_knowledge: Long,
)

@Serializable
data class YnabTransactionDetail(
    val id: String,
    @Serializable(with = LocalDateAsISOSerializer::class)
    val date: LocalDate,
    val amount: Long,
    val memo: String?,
    val cleared: YnabCleared,
    val approved: Boolean,
    val flag_color: YnabFlagColor?,
    val account_id: String,
    val payee_id: String?,
    val category_id: String?,
    val transfer_account_id: String?,
    val transfer_transaction_id: String?,
    val matched_transaction_id: String?,
    val import_id: String?,
    val deleted: Boolean,
    val account_name: String,
    val payee_name: String?,
    val category_name: String?,
    val subtransactions: List<YnabSubTransaction>,
) {
    fun toSaveTransaction(): YnabSaveTransaction {
        val t = this
        return YnabSaveTransaction(
            t.account_id,
            t.date,
            t.amount,
            t.payee_id,
            null,
            t.category_id,
            t.memo,
            t.cleared,
            t.approved,
            t.flag_color,
            t.import_id,
            t.subtransactions.map {
                YnabSaveSubTransaction(
                    it.amount,
                    it.payee_id,
                    it.payee_name,
                    it.category_id,
                    it.memo
                )
            }
        )
    }
}

@Serializable
data class YnabSubTransaction(
    val id: String,
    val transaction_id: String,
    val amount: Long,
    val memo: String?,
    val payee_id: String?,
    val payee_name: String?,
    val category_id: String?,
    val category_name: String?,
    val transfer_account_id: String?,
    val transfer_transaction_id: String?,
    val delete: Boolean,
)

@Serializable
data class YnabErrorResponse(
    val error: YnabErrorDetail,
)

@Serializable
data class YnabErrorDetail(
    val id: String,
    val name: String,
    val detail: String,
)
