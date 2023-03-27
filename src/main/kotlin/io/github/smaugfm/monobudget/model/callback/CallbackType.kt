package io.github.smaugfm.monobudget.model.callback

import kotlin.reflect.KClass

sealed class CallbackType {
    abstract val transactionId: String

    companion object {
        fun classFromCallbackData(callbackData: String?): KClass<out CallbackType>? =
            TransactionUpdateType.classFromCallbackData(callbackData)
                ?: ActionCallbackType.classFromCallbackData(callbackData)
    }
}
