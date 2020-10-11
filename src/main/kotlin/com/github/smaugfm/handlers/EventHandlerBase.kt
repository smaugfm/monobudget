package com.github.smaugfm.handlers

import com.github.smaugfm.events.Dispatch
import com.github.smaugfm.events.Event
import com.github.smaugfm.events.EventHandler
import com.github.smaugfm.events.IEventHandlerCreator

abstract class EventHandlerBase : IEventHandlerCreator {
    final override fun create(dispatch: Dispatch): EventHandler = {
        handle(dispatch, it)
    }

    protected abstract suspend fun handle(dispatch: Dispatch, e: Event): Boolean
}