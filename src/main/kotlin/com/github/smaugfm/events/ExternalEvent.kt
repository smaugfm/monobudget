package com.github.smaugfm.events

import com.github.smaugfm.mono.model.MonoWebHookResponseData

sealed class ExternalEvent {
    sealed class Mono : ExternalEvent() {
        class NewStatementReceived(val data: MonoWebHookResponseData) : Mono()
    }

    sealed class Telegram(val ynabTransactionId: String) : ExternalEvent() {
        class UnclearTransaction(ynabTransactionId: String) : Telegram(ynabTransactionId)
        class MarkTransactionRed(ynabTransactionId: String) : Telegram(ynabTransactionId)
    }
}