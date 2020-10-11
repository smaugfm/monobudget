package com.github.smaugfm.handlers

import com.github.smaugfm.apis.YnabApi
import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event

class YnabHandler(
    private val ynab: YnabApi,
) : EventHandlerBase() {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        if (e !is Event.Ynab)
            return false

        when (e) {
            is Event.Ynab.CreateTransaction -> TODO()
            is Event.Ynab.MarkTransactionUncleared -> TODO()
            is Event.Ynab.MarkTransactionRed -> TODO()
        }
    }
}