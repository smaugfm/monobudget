package io.github.smaugfm.monobudget.common.model.callback

import kotlin.reflect.KClass

sealed class ActionCallbackType : CallbackType() {
    data class ChooseCategory(override val transactionId: String) : ActionCallbackType() {
        companion object : ButtonBase(ChooseCategory::class)
    }

    companion object {
        fun classFromCallbackData(callbackData: String?): KClass<out ActionCallbackType>? =
            ActionCallbackType::class
                .sealedSubclasses.find { callbackData == it.simpleName }
    }
}
