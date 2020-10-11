package com.github.smaugfm.events

import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.ynab.YnabSaveTransaction

sealed class Event {
    sealed class Mono : Event() {
        class NewStatementReceived(val data: MonoWebHookResponseData) : Mono()
    }

    sealed class Ynab() : Event() {
        class CreateTransaction(val transaction: YnabSaveTransaction) : Ynab()
        class MarkTransactionUncleared(val transactionId: String): Ynab()
        class MarkTransactionRed(val transactionId: String): Ynab()
    }

    sealed class Telegram : Event() {
        class SendStatementMessage(
            val telegramChatId: Long,
            val monoStatementItem: MonoStatementItem,
            val transaction: YnabSaveTransaction,
            val categoryName: String,
        ) : Telegram()

        class UnclearTransaction(val ynabTransactionId: String) : Telegram()
        class MarkTransactionRed(val ynabTransactionId: String) : Telegram()
    }
}