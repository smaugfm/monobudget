package com.github.smaugfm.events

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class EventDispatcher(
    vararg registrars: IEventsHandlerRegistrar,
) : IEventDispatcher {
    private val handlers =
        mutableMapOf<Class<*>, IEventHandler<*, *>>().also { handlers ->
            HandlersBuilder(handlers).also { builder ->
                registrars.forEach { it.registerEvents(builder) }
            }
        }

    override suspend fun <R, E : IEvent<R>> invoke(event: E): R {
        @Suppress("UNCHECKED_CAST")
        val handler = handlers[event.javaClass] as? IEventHandler<R, E>
            ?: throw IllegalStateException("No handler found for event $event, ${event.javaClass}")

        return handler.handle(this, event)
    }
}
