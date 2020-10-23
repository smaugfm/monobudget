package com.github.smaugfm.events

import com.github.smaugfm.mono.MonoWebHookResponseData
import com.github.smaugfm.telegram.TransactionActionType
import com.github.smaugfm.ynab.YnabPayee
import com.github.smaugfm.ynab.YnabTransactionDetail

sealed class Event {
    sealed class Mono : Event() {
        data class NewStatementReceived(val data: MonoWebHookResponseData) : Mono(), UnitEvent
    }

    sealed class Ynab : Event() {
        data class TransactionAction(val type: TransactionActionType) : Ynab(), UnitEvent
        object GetPayees : Ynab(), IEvent<List<YnabPayee>>
    }

    sealed class Telegram : Event() {
        data class SendStatementMessage(
            val mono: MonoWebHookResponseData,
            val transaction: YnabTransactionDetail,
        ) : Telegram(), UnitEvent

        data class CallbackQueryReceived(val callbackQueryId: String, val data: String) :
            Telegram(), UnitEvent

        data class AnswerCallbackQuery(val callbackQueryId: String, val text: String?) : Telegram(), UnitEvent
    }
}
