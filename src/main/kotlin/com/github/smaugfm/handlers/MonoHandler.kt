package com.github.smaugfm.handlers

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.mono.MonoStatementItem
import com.github.smaugfm.settings.Mappings
import com.github.smaugfm.ynab.YnabSaveTransaction
import io.michaelrocks.bimap.BiMap
import java.util.logging.Logger

class MonoHandler(
    mappings: Mappings,
) : EventHandlerBase(mappings) {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        return false
    }
}
