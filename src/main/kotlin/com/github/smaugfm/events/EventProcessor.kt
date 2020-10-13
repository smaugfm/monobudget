package com.github.smaugfm.events

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.Executors

class EventProcessor(
    private val handleCreators: List<IEventHandlerCreator>,
) : IEventDispatcher {
    private val handlers = handleCreators.map { it.create(this::dispatch) }
    private val singleThreaded = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val eventsQueue: Queue<Event> = ArrayDeque()
    private var isDispatching: Boolean = false

    override suspend fun dispatch(event: Event) {
        withContext(singleThreaded) {
            eventsQueue.offer(event)
            if (!isDispatching) {
                isDispatching = true
                try {
                    while (eventsQueue.isNotEmpty()) {
                        val current = eventsQueue.poll()

                        for (handler in handlers) {
                            val handled = handler(current)
                            if (handled)
                                break
                        }
                    }
                } finally {
                    isDispatching = false
                }
            }
        }
    }
}
