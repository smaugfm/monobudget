package com.github.smaugfm.handlers

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.settings.Mappings

class MonoHandler(
    mappings: Mappings,
) : EventHandlerBase(mappings) {
    override suspend fun handle(dispatch: Dispatch, e: Event): Boolean {
        return false
    }
}
