package com.github.smaugfm.events

import com.github.smaugfm.util.pp
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
        val eventName = event::class.simpleName

        logger.info("Event $eventName dispatched: ${event.pp()}")
        @Suppress("UNCHECKED_CAST")
        val handler = handlers[event.javaClass] as? IEventHandler<R, E>
            ?: throw IllegalStateException("No handler found for event $event, ${event.javaClass}")

        return try {
            handler.handle(this, event).also {
                if (it is Unit)
                    logger.info("Event $eventName handled.")
                else
                    logger.info("Event ${event::class.simpleName} handled: ${it?.pp()}")
            }
        } catch (e: Throwable) {
            logger.error("Error handling event $eventName.\n", e.pp())
            errorHandler(this, event, e)
            null
        }
    }
}
