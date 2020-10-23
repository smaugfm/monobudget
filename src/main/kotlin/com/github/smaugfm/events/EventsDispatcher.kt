package com.github.smaugfm.events

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

open class EventsDispatcher<T>(
    vararg handlerCreators: IEventHandlerCreator<T>,
) : IEventDispatcher<T> {
    private val handlers = handlerCreators.map { it.create(this::dispatch) }
    private val singleThreadedContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val eventsQueue: Queue<T> = ArrayDeque()
    private var isDispatching: Boolean = false

    override suspend fun dispatch(event: T) {
        logger.info("Event dispatched.\n\t$event")
        withContext(singleThreadedContext) {
            eventsQueue.offer(event)
            if (!isDispatching) {
                isDispatching = true
                try {
                    while (eventsQueue.isNotEmpty()) {
                        val current = eventsQueue.poll()

                        for (handler in handlers) {
                            val handled = handler.handle(current)
                            if (handled) {
                                logger.info("Event handled by ${handler.name}")
                                break
                            }
                        }
                    }
                } finally {
                    isDispatching = false
                }
            }
        }
    }
}
