package com.github.smaugfm.events

import kotlinx.coroutines.Deferred

interface IEventHandler<R, T : IEvent<R>> {
    suspend fun handleAsync(dispatcher: IEventDispatcher, event: T): Deferred<R>
}

interface IEventsHandlerRegistrar {
    fun registerEvents(builder: HandlersBuilder)
}

interface IEvent<out T>

typealias UnitEvent = IEvent<Unit>

interface IEventDispatcher {
    suspend operator fun <R, E : IEvent<R>> invoke(event: E): R
}
