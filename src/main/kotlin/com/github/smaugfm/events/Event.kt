package com.github.smaugfm.events

import com.github.smaugfm.handlers.TelegramHandler
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.ynab.YnabTransactionDetail

sealed class Event {
    sealed class Mono : Event() {
        class NewStatementReceived(val data: MonoWebHookResponseData) : Mono()
    }

    sealed class Ynab() : Event() {
        class UpdateTransaction(val transactionId: String, val type: TelegramHandler.Companion.UpdateType): Ynab()
    }

    sealed class Telegram : Event() {
        class SendStatementMessage(
            val mono: MonoWebHookResponseData,
            val transaction: YnabTransactionDetail,
        ) : Telegram()

        class CallbackQueryReceived(val data: String): Telegram()
    }
}