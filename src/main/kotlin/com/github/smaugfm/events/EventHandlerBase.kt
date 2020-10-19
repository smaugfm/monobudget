package com.github.smaugfm.events

import com.github.smaugfm.settings.Mappings

abstract class EventHandlerBase(protected val mappings: Mappings) : IEventHandlerCreator<Event> {
    final override fun create(dispatch: Dispatch): EventHandler = {
        handle(dispatch, it)
    }

    protected abstract suspend fun handle(dispatch: Dispatch, e: Event): Boolean
}
