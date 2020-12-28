package com.github.smaugfm.events

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

open class EventDispatcher(
    private val errorHandler: suspend (dispatcher: IEventDispatcher, event: IEvent<*>, exception: Throwable) -> Unit,
    vararg registrars: IEventsHandlerRegistrar,
) : IEventDispatcher {
    private val handlers =
        mutableMapOf<Class<*>, IEventHandler<*, *>>().also { handlers ->
            HandlersBuilder(handlers).also { builder ->
                registrars.forEach { it.registerEvents(builder) }
            }
        }

    override suspend fun <R, E : IEvent<R>> invoke(event: E): R? {
        @Suppress("UNCHECKED_CAST")
        logger.info("Event dispatched: $event")
        val handler = handlers[event.javaClass] as? IEventHandler<R, E>
            ?: throw IllegalStateException("No handler found for event $event, ${event.javaClass}")

        return try {
            handler.handle(this, event).also {
                logger.info("Event handled: $it")
            }
        } catch (e: Throwable) {
            logger.error("Error handling event.", e)
            errorHandler(this, event, e)
            null
        }
    }
}
