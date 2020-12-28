package com.github.smaugfm.events

interface IEventHandler<R, T : IEvent<R>> {
    suspend fun handle(dispatcher: IEventDispatcher, event: T): R
}

interface IEventsHandlerRegistrar {
    fun registerEvents(builder: HandlersBuilder)
}

open class CompositeHandler(val handlers: Collection<IEventsHandlerRegistrar>) : IEventsHandlerRegistrar {
    override fun registerEvents(builder: HandlersBuilder) {
        handlers.forEach { it.registerEvents(builder) }
    }
}

abstract class Handler : IEventsHandlerRegistrar {
    final override fun registerEvents(builder: HandlersBuilder) {
        builder.apply {
            registerHandlerFunctions()
        }
    }

    abstract fun HandlersBuilder.registerHandlerFunctions()
}

interface IEvent<out T>

typealias UnitEvent = IEvent<Unit>

interface IEventDispatcher {
    suspend operator fun <R, E : IEvent<R>> invoke(event: E): R?
}
