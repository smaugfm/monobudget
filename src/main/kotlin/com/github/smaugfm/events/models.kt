package com.github.smaugfm.events

interface IEventHandler<R, T : IEvent<R>> {
    suspend fun handle(dispatcher: IEventDispatcher, event: T): R
}

interface IEventsHandlerRegistrar {
    fun registerEvents(builder: HandlersBuilder)
}

interface IEvent<out T>

typealias UnitEvent = IEvent<Unit>

interface IEventDispatcher {
    suspend operator fun <R, E : IEvent<R>> invoke(event: E): R
}
