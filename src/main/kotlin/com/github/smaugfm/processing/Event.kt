package com.github.smaugfm.processing

import com.github.smaugfm.mono.model.MonoWebHookResponseData

sealed class Event {
    class NewStatement(val data: MonoWebHookResponseData): Event()
    sealed class TelegramAction(val ynabTransactionId: String): Event() {
        class UnclearTransaction(ynabTransactionId: String): TelegramAction(ynabTransactionId)
        class MarkTransactionRed(ynabTransactionId: String): TelegramAction(ynabTransactionId)
    }
}