package com.github.smaugfm.handlers

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.EventHandler
import com.github.smaugfm.events.IEventHandlerCreator
import com.github.smaugfm.settings.Mappings

abstract class EventHandlerBase(protected val mappings: Mappings) : IEventHandlerCreator {
    final override fun create(dispatch: Dispatch): EventHandler = {
        handle(dispatch, it)
    }

    protected abstract suspend fun handle(dispatch: Dispatch, e: Event): Boolean
}
