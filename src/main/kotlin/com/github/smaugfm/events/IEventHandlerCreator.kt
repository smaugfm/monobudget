package com.github.smaugfm.events

interface IEventHandlerCreator {
    fun create(dispatch: Dispatch): EventHandler
}

typealias EventHandler = suspend (Event) -> Boolean
typealias Dispatch = suspend (Event) -> Unit
