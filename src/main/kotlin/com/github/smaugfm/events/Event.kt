package com.github.smaugfm.events

import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.ynab.YnabTransactionDetail

sealed class Event {
    sealed class Mono : Event() {
        data class NewStatementReceived(val data: MonoWebHookResponseData) : Mono()
    }

    sealed class Ynab : Event() {
        data class UpdateTransaction(val transactionId: String, val type: TelegramHandler.Companion.UpdateType) : Ynab()
    }

    sealed class Telegram : Event() {
        data class SendStatementMessage(
            val mono: MonoWebHookResponseData,
            val transaction: YnabTransactionDetail,
        ) : Telegram()

        data class CallbackQueryReceived(val telegramChatId: Int, val data: String) : Telegram()
    }
}
