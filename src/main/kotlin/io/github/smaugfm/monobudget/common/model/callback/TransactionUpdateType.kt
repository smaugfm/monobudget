package io.github.smaugfm.monobudget.common.model.callback

import com.elbekd.bot.types.InlineKeyboardButton
import kotlin.reflect.KClass

sealed class TransactionUpdateType : CallbackType() {
    data class Uncategorize(override val transactionId: String) : TransactionUpdateType() {
        companion object : ButtonBase(Uncategorize::class)
    }

    data class Unapprove(override val transactionId: String) : TransactionUpdateType() {
        companion object : ButtonBase(Unapprove::class)
    }

    data class MakePayee(override val transactionId: String, val payee: String) : TransactionUpdateType() {
        companion object : ButtonBase(MakePayee::class)
    }

    data class UpdateCategory(override val transactionId: String, val categoryId: String) : TransactionUpdateType() {
        companion object {
            private const val DELIMITER = "#"
            fun button(categoryId: String, categoryName: String) = InlineKeyboardButton(
                categoryName,
                callbackData = "${UpdateCategory::class.simpleName}$DELIMITER$categoryId"
            )

            fun extractCategoryIdFromCallbackData(callbackData: String) = callbackData.split(DELIMITER)[1]
        }
    }

    companion object {
        fun classFromCallbackData(callbackData: String?): KClass<out TransactionUpdateType>? =
            TransactionUpdateType::class
                .sealedSubclasses.find { callbackData?.startsWith(it.simpleName!!) ?: false }
    }
}
