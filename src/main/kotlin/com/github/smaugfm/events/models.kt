package com.github.smaugfm.events

interface IEventHandlerCreator<T> {
    fun create(dispatch: GenericDispatch<T>): GenericEventHandler<T>
}

typealias GenericEventHandler<T> = suspend (T) -> Boolean
typealias EventHandler = GenericEventHandler<Event>
typealias GenericDispatch<T> = suspend (T) -> Unit
typealias Dispatch = GenericDispatch<Event>
