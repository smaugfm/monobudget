package com.github.smaugfm.events

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.Stack
import java.util.concurrent.Executors

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

    private val singleThreadedContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val eventsStack: Stack<Pair<IEvent<*>, Deferred<*>>> = Stack()

    override suspend fun <R, E : IEvent<R>> invoke(event: E): R {

        @Suppress("UNCHECKED_CAST")
        val handler = handlers[event.javaClass] as? IEventHandler<R, E>
            ?: throw IllegalStateException("No handler found for event $event, ${event.javaClass}")

        withContext(singleThreadedContext) {
            check(eventsStack.size <= MAX_DISPATCH_STACK_DEPTH) { "Max events stack depth reached" }
            logger.info("Event dispatched.\n\t$event")
            eventsStack.push(Pair(event, handler.handleAsync(this@EventDispatcher, event)))
        }

        @Suppress("UNCHECKED_CAST")
        return eventsStack.pop().second.await() as R
    }

    companion object {
        const val MAX_DISPATCH_STACK_DEPTH = 1000
    }
}
