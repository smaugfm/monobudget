package com.github.smaugfm.events

import com.elbekD.bot.types.CallbackQuery
import com.elbekD.bot.types.ReplyKeyboard
import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.ynab.YnabTransactionDetail

sealed class Event {
    sealed class Mono : Event() {
        data class NewStatementReceived(val data: MonoWebHookResponseData) : Mono(), UnitEvent
    }

    sealed class Ynab : Event() {
        data class TransactionAction(
            val type: TransactionActionType,
        ) : Ynab(), IEvent<YnabTransactionDetail>
    }

    sealed class Telegram : Event() {
        data class SendStatementMessage(
            val mono: MonoWebHookResponseData,
            val transaction: YnabTransactionDetail,
        ) : Telegram(), UnitEvent
        data class CallbackQueryReceived(val callbackQuery: CallbackQuery) : Telegram(), UnitEvent
        data class SendHTMLMessage(
            val chatId: Int,
            val msg: String,
            val keyboard: ReplyKeyboard?
        ) : Telegram(), UnitEvent
    }
}
