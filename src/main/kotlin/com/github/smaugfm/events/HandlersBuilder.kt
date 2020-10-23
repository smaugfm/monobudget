package com.github.smaugfm.events

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class HandlersBuilder(private val handlers: MutableMap<Class<*>, IEventHandler<*, *>>) {
    fun <R, T : IEvent<R>> register(eventCls: Class<T>, handler: IEventHandler<R, T>) {
        if (handlers.containsKey(eventCls))
            throw IllegalArgumentException("Cannot register another handler for event ${eventCls.simpleName}")
        handlers[eventCls] = handler
    }

    inline fun <R, reified T : IEvent<R>> register(
        crossinline handler: suspend (dispatcher: IEventDispatcher, event: T) -> Deferred<R>,
    ) {
        register(
            T::class.java,
            object : IEventHandler<R, T> {
                override suspend fun handleAsync(dispatcher: IEventDispatcher, event: T): Deferred<R> {
                    return handler(dispatcher, event)
                }
            }
        )
    }

    inline fun <R, reified T : IEvent<R>> register(crossinline handler: suspend (event: T) -> Deferred<R>) {
        register(
            T::class.java,
            object : IEventHandler<R, T> {
                override suspend fun handleAsync(dispatcher: IEventDispatcher, event: T): Deferred<R> {
                    return handler(event)
                }
            }
        )
    }

    inline fun <reified T : IEvent<Unit>> registerUnit(
        crossinline handler: suspend (dispatcher: IEventDispatcher, event: T) -> Unit,
    ) {
        register(
            T::class.java,
            object : IEventHandler<Unit, T> {
                override suspend fun handleAsync(dispatcher: IEventDispatcher, event: T): Deferred<Unit> {
                    handler(dispatcher, event)
                    return CompletableDeferred(Unit)
                }
            }
        )
    }

    inline fun <reified T : IEvent<Unit>> registerUnit(crossinline handler: suspend (event: T) -> Unit) {
        register(
            T::class.java,
            object : IEventHandler<Unit, T> {
                override suspend fun handleAsync(dispatcher: IEventDispatcher, event: T): Deferred<Unit> {
                    handler(event)
                    return CompletableDeferred(Unit)
                }
            }
        )
    }
}
