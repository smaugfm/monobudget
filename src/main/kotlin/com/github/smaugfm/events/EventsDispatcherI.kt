package com.github.smaugfm.events

import io.ktor.util.error
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.Executors

private val logger = KotlinLogging.logger {}

open class EventsDispatcherI<T>(
    vararg handlerCreators: EventHandlerCreator<T>,
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
                            try {
                                val handled = handler.handle(current)
                                if (handled) {
                                    logger.info("Event handled by ${handler.name}")
                                    break
                                }
                            } catch (e: Throwable) {
                                logger.info("Error in event handler ${handler.name}")
                                logger.info(event.toString())
                                logger.error(e)
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
